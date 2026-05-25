package org.openrndr.extra.rtree

import org.openrndr.collections.PriorityQueue
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
