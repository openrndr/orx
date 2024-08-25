package org.openrndr.extra.quadtree

import org.openrndr.draw.Drawer
import org.openrndr.draw.RectangleBatchBuilder
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.intersects
import kotlin.jvm.JvmRecord

@JvmRecord
data class QuadtreeQuery<T>(val nearest: T, val neighbours: List<T>, val quads: List<Quadtree<T>>)

/**
 * Quadtree
 *
 * @param T
 * @property bounds the tree's bounding box
 * @property maxObjects maximum number of objects per node
 * @property mapper
 */
class Quadtree<T>(val bounds: Rectangle, val maxObjects: Int = 10, val mapper: ((T) -> Vector2)) : IQuadtree<T> {
    /**
     * The 4 nodes of the tree
     */
    val nodes = arrayOfNulls<Quadtree<T>>(4)
    var depth = 0
    val objects = mutableListOf<T>()

    private val isLeaf: Boolean
        get() = nodes[0] == null

    /**
     * Clears the whole tree
     */
    override fun clear() {
        objects.clear()

        for (i in nodes.indices) {
            nodes[i]?.let {
                it.clear()

                nodes[i] = null
            }
        }
    }

    /**
     * Finds the nearest and neighbouring objects within a radius
     * (needs to have a different name so there is no ambiguity when the generic object type is Vector2)
     *
     * @param point
     * @param radius
     * @return
     */
    override fun nearestToPoint(point: Vector2, radius: Double): QuadtreeQuery<T>? {
        if (!bounds.contains(point)) return null

        val r2 = radius * radius

        val scaledBounds = Rectangle.fromCenter(point, radius * 2)
        val intersected: List<Quadtree<T>> = intersect(scaledBounds) ?: return null

        var minDist = Double.MAX_VALUE
        val nearestObjects = mutableListOf<T>()
        var nearestObject: T? = null

        for (interNode in intersected) {
            for (obj in interNode.objects) {
                val p = mapper(obj)

                val dist = p.squaredDistanceTo(point)

                if (dist < r2) {
                    nearestObjects.add(obj)

                    if (dist < minDist) {
                        minDist = dist
                        nearestObject = obj
                    }
                }
            }
        }

        if (nearestObject == null) return null

        return QuadtreeQuery(nearestObject, nearestObjects, intersected)
    }

    /**
     * Finds the nearest and neighbouring points within a radius
     *
     * @param element
     * @param radius
     * @return
     */
    override fun nearest(element: T, radius: Double): QuadtreeQuery<T>? {
        val point = mapper(element)

        if (!bounds.contains(point)) return null

        val r2 = radius * radius

        val scaledBounds = Rectangle.fromCenter(point, radius * 2)
        val intersected: List<Quadtree<T>> = intersect(scaledBounds) ?: return null

        var minDist = Double.MAX_VALUE
        val nearestObjects = mutableListOf<T>()
        var nearestObject: T? = null

        for (interNode in intersected) {
            for (obj in interNode.objects) {
                if (element === obj) continue
                val p = mapper(obj)

                val dist = p.squaredDistanceTo(point)

                if (dist < r2) {
                    nearestObjects.add(obj)

                    if (dist < minDist) {
                        minDist = dist
                        nearestObject = obj
                    }
                }
            }
        }

        if (nearestObject == null) return null

        return QuadtreeQuery(nearestObject, nearestObjects, intersected)
    }

    /**
     * Inserts the element in the appropriate node
     *
     * @param element
     * @return
     */
    override fun insert(element: T): Boolean {
        // only* the root needs to check this
        if (depth == 0) {
            if (!bounds.contains(mapper(element))) return false
        }

        if ((objects.size < maxObjects && isLeaf)) {
            objects.add(element)

            return true
        }

        if (isLeaf) subdivide()

        objects.add(element)

        for (obj in objects) {
            val p = mapper(obj)
            val x = if (p.x > bounds.center.x) 1 else 0
            val y = if (p.y > bounds.center.y) 1 else 0
            val nodeIndex = x + y * 2

            nodes[nodeIndex]?.insert(obj)
        }

        objects.clear()

        return true
    }

    override fun remove(element: T): Boolean {
        if (isLeaf) {
            return objects.remove(element)
        }
        val p = mapper(element)
        val x = if (p.x > bounds.center.x) 1 else 0
        val y = if (p.y > bounds.center.y) 1 else 0
        val nodeIndex = x + y * 2
        return nodes[nodeIndex]!!.remove(element)
    }

    /**
     * Finds which node the element is within (but not necessarily belonging to)
     *
     * @param element
     * @return
     */
    override fun findNode(element: T): Quadtree<T>? {
        val v = mapper(element)

        if (!bounds.contains(v)) return null

        if (isLeaf) return this

        for (node in nodes) {
            node?.findNode(element)?.let { return it }
        }

        return null
    }

    /**
     * Draw the quadtree using batching
     *
     * @param batchBuilder
     */
    fun batch(batchBuilder: RectangleBatchBuilder) {
        batchBuilder.rectangle(bounds)

        for (node in nodes) {
            node?.batch(batchBuilder)
        }
    }

    /**
     * Draw the quadtree
     *
     * @param drawer
     */
    fun draw(drawer: Drawer) {
        drawer.rectangle(bounds)

        for (node in nodes) {
            node?.draw(drawer)
        }
    }

    private fun intersect(rect: Rectangle): List<Quadtree<T>>? {
        val intersects = bounds.intersects(rect)

        if (!intersects) return null

        if (isLeaf) return listOf(this)

        val intersected = mutableListOf<Quadtree<T>>()

        for (node in nodes) {
            if (node != null) node.intersect(rect)?.let {
                intersected.addAll(it)
            }
        }

        return intersected
    }

    private fun subdivide() {
        val width = bounds.center.x - bounds.corner.x
        val height = bounds.center.y - bounds.corner.y

        val newDepth = depth + 1

        var node = Quadtree(Rectangle(bounds.corner, width, height), maxObjects, mapper)
        node.depth = newDepth
        nodes[0] = node

        node = Quadtree(Rectangle(Vector2(bounds.center.x, bounds.corner.y), width, height), maxObjects, mapper)
        node.depth = newDepth
        nodes[1] = node

        node = Quadtree(Rectangle(Vector2(bounds.corner.x, bounds.center.y), width, height), maxObjects, mapper)
        node.depth = newDepth
        nodes[2] = node

        node = Quadtree(Rectangle(bounds.center, width, height), maxObjects, mapper)
        node.depth = newDepth
        nodes[3] = node
    }

    override fun toString(): String {
        return "QuadTree { objects: ${objects.size}, depth: $depth, isLeaf: $isLeaf"
    }
}