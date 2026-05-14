package org.openrndr.extra.rtree

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.collections.PriorityQueue
import kotlin.math.*

typealias Polygon2D = List<Vector2>

fun Polygon2D.bounds(): Rectangle {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (v in this) {
        minX = min(minX, v.x)
        minY = min(minY, v.y)
        maxX = max(maxX, v.x)
        maxY = max(maxY, v.y)
    }
    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}

/**
 * R-tree implementation for 2D spatial indexing.
 */
class RTree<T>(
    val minEntries: Int = 2,
    val maxEntries: Int = 4,
    val getBounds: (T) -> Rectangle
) {
    private var root: RTreeNode<T> = RTreeNode<T>(true)



    fun insert(item: T) {
        val bounds = getBounds(item)
        val leaf = chooseLeaf(root, bounds)
        leaf.entries.add(RTreeEntry(bounds, item))
        leaf.updateBounds()
        if (leaf.entries.size > maxEntries) {
            splitNode(leaf)
        }
    }

    fun delete(item: T) {
        val bounds = getBounds(item)
        val leaf = findLeaf(root, item, bounds)
        if (leaf != null) {
            val index = leaf.entries.indexOfFirst { it.item == item }
            if (index != -1) {
                leaf.entries.removeAt(index)
                condenseTree(leaf)
                if (root.children.size == 1 && !root.isLeaf) {
                    root = root.children[0]
                    root.parent = null
                }
            }
        }
    }

    private fun findLeaf(node: RTreeNode<T>, item: T, bounds: Rectangle): RTreeNode<T>? {
        if (node.isLeaf) {
            if (node.entries.any { it.item == item }) {
                return node
            }
            return null
        }

        for (child in node.children) {
            if (child.bounds.intersects(bounds)) {
                val leaf = findLeaf(child, item, bounds)
                if (leaf != null) return leaf
            }
        }
        return null
    }

    private fun condenseTree(node: RTreeNode<T>) {
        val eliminatedNodes = mutableListOf<RTreeNode<T>>()
        var n: RTreeNode<T>? = node
        while (n != root) {
            val parent = n!!.parent!!
            if (n.isLeaf) {
                if (n.entries.size < minEntries) {
                    parent.children.remove(n)
                    eliminatedNodes.add(n)
                }
            } else {
                if (n.children.size < minEntries) {
                    parent.children.remove(n)
                    eliminatedNodes.add(n)
                }
            }
            parent.updateBounds()
            n = parent
        }

        for (eliminated in eliminatedNodes) {
            if (eliminated.isLeaf) {
                for (entry in eliminated.entries) {
                    insert(entry.item!!)
                }
            } else {
                fun collectLeaves(en: RTreeNode<T>) {
                    if (en.isLeaf) {
                        for (entry in en.entries) {
                            insert(entry.item!!)
                        }
                    } else {
                        for (child in en.children) {
                            collectLeaves(child)
                        }
                    }
                }
                collectLeaves(eliminated)
            }
        }
    }

    private fun chooseLeaf(node: RTreeNode<T>, bounds: Rectangle): RTreeNode<T> {
        if (node.isLeaf) return node

        var bestNode: RTreeNode<T>? = null
        var minEnlargement = Double.POSITIVE_INFINITY
        var minArea = Double.POSITIVE_INFINITY

        for (child in node.children) {
            val childBounds = child.bounds
            val enlargedBounds = childBounds.unioned(bounds)
            val enlargement = enlargedBounds.area - childBounds.area
            if (enlargement < minEnlargement) {
                minEnlargement = enlargement
                minArea = childBounds.area
                bestNode = child
            } else if (enlargement == minEnlargement) {
                if (childBounds.area < minArea) {
                    minArea = childBounds.area
                    bestNode = child
                }
            }
        }
        return chooseLeaf(bestNode!!, bounds)
    }

    private fun splitNode(node: RTreeNode<T>) {
        val entries = if (node.isLeaf) {
            node.entries.map { RTreeEntry(it.bounds, it.item) }
        } else {
            node.children.map { RTreeEntry(it.bounds, null, it) }
        }

        val (group1, group2) = quadraticSplit(entries)

        node.entries.clear()
        node.children.clear()

        if (node.isLeaf) {
            node.entries.addAll(group1.map { RTreeEntry(it.bounds, it.item!!) })
        } else {
            node.children.addAll(group1.map { it.child!! })
            for (child in node.children) child.parent = node
        }
        node.updateBounds()

        val newNode = RTreeNode<T>(node.isLeaf)
        newNode.parent = node.parent
        if (node.isLeaf) {
            newNode.entries.addAll(group2.map { RTreeEntry(it.bounds, it.item!!) })
        } else {
            newNode.children.addAll(group2.map { it.child!! })
            for (child in newNode.children) child.parent = newNode
        }
        newNode.updateBounds()

        val parent = node.parent
        if (parent == null) {
            val newRoot = RTreeNode<T>(false)
            newRoot.children.add(node)
            newRoot.children.add(newNode)
            node.parent = newRoot
            newNode.parent = newRoot
            newRoot.updateBounds()
            root = newRoot
        } else {
            parent.children.add(newNode)
            parent.updateBounds()
            if (parent.children.size > maxEntries) {
                splitNode(parent)
            }
        }
    }

    private fun quadraticSplit(entries: List<RTreeEntry<T>>): Pair<List<RTreeEntry<T>>, List<RTreeEntry<T>>> {
        // Pick seeds
        var maxWaste = Double.NEGATIVE_INFINITY
        var seed1Idx = 0
        var seed2Idx = 1

        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val unionArea = entries[i].bounds.unioned(entries[j].bounds).area
                val waste = unionArea - entries[i].bounds.area - entries[j].bounds.area
                if (waste > maxWaste) {
                    maxWaste = waste
                    seed1Idx = i
                    seed2Idx = j
                }
            }
        }

        val group1 = mutableListOf(entries[seed1Idx])
        val group2 = mutableListOf(entries[seed2Idx])
        var group1Bounds = entries[seed1Idx].bounds
        var group2Bounds = entries[seed2Idx].bounds

        val remaining = entries.toMutableList()
        remaining.removeAt(max(seed1Idx, seed2Idx))
        remaining.removeAt(min(seed1Idx, seed2Idx))

        while (remaining.isNotEmpty()) {
            if (group1.size + remaining.size <= minEntries) {
                group1.addAll(remaining)
                remaining.clear()
                break
            }
            if (group2.size + remaining.size <= minEntries) {
                group2.addAll(remaining)
                remaining.clear()
                break
            }

            var maxDiff = Double.NEGATIVE_INFINITY
            var bestIdx = 0
            for (i in remaining.indices) {
                val d1 = group1Bounds.unioned(remaining[i].bounds).area - group1Bounds.area
                val d2 = group2Bounds.unioned(remaining[i].bounds).area - group2Bounds.area
                val diff = abs(d1 - d2)
                if (diff > maxDiff) {
                    maxDiff = diff
                    bestIdx = i
                }
            }

            val next = remaining.removeAt(bestIdx)
            val d1 = group1Bounds.unioned(next.bounds).area - group1Bounds.area
            val d2 = group2Bounds.unioned(next.bounds).area - group2Bounds.area

            if (d1 < d2) {
                group1.add(next)
                group1Bounds = group1Bounds.unioned(next.bounds)
            } else if (d2 < d1) {
                group2.add(next)
                group2Bounds = group2Bounds.unioned(next.bounds)
            } else {
                if (group1Bounds.area < group2Bounds.area) {
                    group1.add(next)
                    group1Bounds = group1Bounds.unioned(next.bounds)
                } else {
                    group2.add(next)
                    group2Bounds = group2Bounds.unioned(next.bounds)
                }
            }
        }
        return group1 to group2
    }

    fun findInRange(area: Rectangle): List<T> {
        val results = mutableListOf<T>()
        fun search(node: RTreeNode<T>) {
            if (node.bounds.intersects(area)) {
                if (node.isLeaf) {
                    for (entry in node.entries) {
                        if (entry.bounds.intersects(area)) {
                            results.add(entry.item!!)
                        }
                    }
                } else {
                    for (child in node.children) {
                        search(child)
                    }
                }
            }
        }
        search(root)
        return results
    }

    fun findKNearest(query: Vector2, k: Int, distanceFunc: (T, Vector2) -> Double): List<T> {
        val result = mutableListOf<T>()
        val queue = PriorityQueue<SearchEntry<T>>(compareBy { it.minDist })
        queue.add(SearchEntry(root.bounds.squaredDistanceTo(query), null, root))

        while (queue.size() > 0 && result.size < k) {
            val entry = queue.poll()!!
            if (entry.item != null) {
                result.add(entry.item)
            } else if (entry.node != null) {
                val node = entry.node
                if (node.isLeaf) {
                    for (e in node.entries) {
                        queue.add(SearchEntry(distanceFunc(e.item!!, query), e.item, null))
                    }
                } else {
                    for (child in node.children) {
                        queue.add(SearchEntry(child.bounds.squaredDistanceTo(query), null, child))
                    }
                }
            }
        }
        return result
    }

    private data class SearchEntry<T>(val minDist: Double, val item: T?, val node: RTreeNode<T>?)

    /**
     * Finds the nearest point that is not inside any polygon in the R-tree.
     * This is a simplified version that checks distances to polygon edges and boundaries.
     * @return a pair of position and a rectangle representing a lower bound of the open space size
     */
    fun nearestOpenSpace(
        query: Vector2,
        isInside: (T, Vector2) -> Boolean,
        nearestPointOnBoundary: (T, Vector2) -> Vector2
    ): Pair<Vector2, Rectangle> {
        var inside = false
        var firstEmptyBound: Rectangle = Rectangle(
            -1.0E18,
            -1.0E18,
            2.0E18,
            2.0E18
        )

        fun checkInside(node: RTreeNode<T>) {
            if (inside) return
            if (node.bounds.contains(query)) {
                firstEmptyBound = node.bounds
                if (node.isLeaf) {
                    for (entry in node.entries) {
                        if (entry.bounds.contains(query)) {
                            if (isInside(entry.item!!, query)) {
                                inside = true
                                return
                            }
                        }
                    }
                } else {
                    for (child in node.children) {
                        checkInside(child)
                        if (inside) return
                    }
                }
            }
        }
        checkInside(root)

        if (!inside) return Pair(query, firstEmptyBound)

        // If inside, we need to find the nearest point on the boundary of ANY polygon
        // We can use the k-NN logic to find the nearest polygon and then its nearest boundary point.
        // For simplicity, let's find the nearest boundary point among all candidates.
        val kNearest = findKNearest(query, 10) { item, q ->
            val p = nearestPointOnBoundary(item, q)
            (p - q).squaredLength
        }

        if (kNearest.isEmpty()) return Pair(query, firstEmptyBound)

        var minSqDist = Double.POSITIVE_INFINITY
        var bestPoint = query

        for (item in kNearest) {
            val p = nearestPointOnBoundary(item, query)
            val d2 = (p - query).squaredLength
            if (d2 < minSqDist) {
                minSqDist = d2
                bestPoint = p
            }
        }

        return Pair(bestPoint, firstEmptyBound)
    }
}

private class RTreeNode<T>(val isLeaf: Boolean) {
    var bounds: Rectangle = Rectangle(0.0, 0.0, 0.0, 0.0)
    val entries = mutableListOf<RTreeEntry<T>>()
    val children = mutableListOf<RTreeNode<T>>()
    var parent: RTreeNode<T>? = null

    fun updateBounds() {
        val b = if (isLeaf) {
            if (entries.isEmpty()) Rectangle(0.0, 0.0, 0.0, 0.0)
            else {
                var minX = Double.POSITIVE_INFINITY
                var minY = Double.POSITIVE_INFINITY
                var maxX = Double.NEGATIVE_INFINITY
                var maxY = Double.NEGATIVE_INFINITY
                for (e in entries) {
                    minX = min(minX, e.bounds.x)
                    minY = min(minY, e.bounds.y)
                    maxX = max(maxX, e.bounds.x + e.bounds.width)
                    maxY = max(maxY, e.bounds.y + e.bounds.height)
                }
                Rectangle(minX, minY, maxX - minX, maxY - minY)
            }
        } else {
            if (children.isEmpty()) Rectangle(0.0, 0.0, 0.0, 0.0)
            else {
                var minX = Double.POSITIVE_INFINITY
                var minY = Double.POSITIVE_INFINITY
                var maxX = Double.NEGATIVE_INFINITY
                var maxY = Double.NEGATIVE_INFINITY
                for (c in children) {
                    minX = min(minX, c.bounds.x)
                    minY = min(minY, c.bounds.y)
                    maxX = max(maxX, c.bounds.x + c.bounds.width)
                    maxY = max(maxY, c.bounds.y + c.bounds.height)
                }
                Rectangle(minX, minY, maxX - minX, maxY - minY)
            }
        }
        bounds = b
        parent?.updateBounds()
    }
}

private data class RTreeEntry<T>(val bounds: Rectangle, val item: T? = null, val child: RTreeNode<T>? = null)

private fun Rectangle.unioned(other: Rectangle): Rectangle {
    val minX = min(x, other.x)
    val minY = min(y, other.y)
    val maxX = max(x + width, other.x + other.width)
    val maxY = max(y + height, other.y + other.height)
    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}


private fun Rectangle.squaredDistanceTo(query: Vector2): Double {
    val dx = max(0.0, max(x - query.x, query.x - (x + width)))
    val dy = max(0.0, max(y - query.y, query.y - (y + height)))
    return dx * dx + dy * dy
}

private fun Rectangle.contains(v: Vector2): Boolean {
    return v.x >= x && v.x <= x + width && v.y >= y && v.y <= y + height
}

class RtreePolygon2D(minEntries: Int = 2, maxEntries: Int = 4) {
    private val rtree = RTree<Polygon2D>(minEntries, maxEntries) { it.bounds() }

    fun insert(polygon: Polygon2D) = rtree.insert(polygon)
    fun delete(polygon: Polygon2D) = rtree.delete(polygon)

    fun findInRange(area: Rectangle): List<Polygon2D> = rtree.findInRange(area)



    fun findKNearest(query: Vector2, k: Int): List<Polygon2D> {
        return rtree.findKNearest(query, k) { poly, q ->
            // Closest distance from point to polygon
            var minSqDist = Double.POSITIVE_INFINITY
            for (i in poly.indices) {
                val p1 = poly[i]
                val p2 = poly[(i + 1) % poly.size]
                val d2 = squaredDistanceToSegment(q, p1, p2)
                if (d2 < minSqDist) minSqDist = d2
            }
            minSqDist
        }
    }

    fun findNearestOpenSpace(query: Vector2): Pair<Vector2, Rectangle> {
        return rtree.nearestOpenSpace(query,
            isInside = { poly, q ->
                // PNPOLY algorithm
                var inside = false
                var j = poly.size - 1
                for (i in poly.indices) {
                    if (((poly[i].y > q.y) != (poly[j].y > q.y)) &&
                        (q.x < (poly[j].x - poly[i].x) * (q.y - poly[i].y) / (poly[j].y - poly[i].y) + poly[i].x)) {
                        inside = !inside
                    }
                    j = i
                }
                inside
            },
            nearestPointOnBoundary = { poly, q ->
                var minSqDist = Double.POSITIVE_INFINITY
                var bestPoint = poly[0]
                for (i in poly.indices) {
                    val p1 = poly[i]
                    val p2 = poly[(i + 1) % poly.size]
                    val p = nearestPointOnSegment(q, p1, p2)
                    val d2 = (p - q).squaredLength
                    if (d2 < minSqDist) {
                        minSqDist = d2
                        bestPoint = p
                    }
                }
                bestPoint
            }
        )
    }
}

private fun squaredDistanceToSegment(p: Vector2, a: Vector2, b: Vector2): Double {
    val ab = b - a
    val ap = p - a
    val bp = p - b
    val e = ap.dot(ab)
    if (e <= 0) return ap.squaredLength
    val f = ab.dot(ab)
    if (e >= f) return bp.squaredLength
    return ap.squaredLength - e * e / f
}

private fun nearestPointOnSegment(p: Vector2, a: Vector2, b: Vector2): Vector2 {
    val ab = b - a
    val ap = p - a
    val t = ap.dot(ab) / ab.dot(ab)
    return when {
        t < 0.0 -> a
        t > 1.0 -> b
        else -> a + ab * t
    }
}

