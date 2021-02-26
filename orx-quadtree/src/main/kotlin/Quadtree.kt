package quadtree

import org.openrndr.draw.Drawer
import org.openrndr.draw.RectangleBatchBuilder
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.intersects

data class QuadTreeQuery<T>(val nearest: T, val neighbours: List<T>, val quads: List<QuadTree<T>>)

class QuadTree<T>(val bounds: Rectangle, val maxPoints: Int = 10, val mapper: ((T) -> Vector2)) {
    val nodes = arrayOfNulls<QuadTree<T>>(4)
    var depth = 0
    val objects = mutableListOf<T>()

    private val isLeaf: Boolean
        get() = nodes[0] == null

    fun clear() {
        objects.clear()

        for (i in nodes.indices) {
            nodes[i]?.let {
                it.clear()

                nodes[i] = null
            }
        }
    }

    fun nearest(element: T, radius: Double): QuadTreeQuery<T>? {
        val point = mapper(element)

        if (!bounds.contains(point)) return null

        val r2 = radius * radius

        val scaledBounds = Rectangle.fromCenter(point, radius * 2)
        val intersected: List<QuadTree<T>> = intersect(scaledBounds) ?: return null

        var minDist = Double.MAX_VALUE
        val nearestObjects = mutableListOf<T>()
        var nearestObject: T? = null

        for (interNode in intersected) {
            for (obj in interNode.objects) {
                if (element == obj) continue
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

        return QuadTreeQuery(nearestObject, nearestObjects, intersected)
    }

    private fun intersect(rect: Rectangle): List<QuadTree<T>>? {
        val intersects = intersects(bounds, rect)

        if (!intersects) return null

        if (isLeaf) return listOf(this)

        val intersected = mutableListOf<QuadTree<T>>()

        for (node in nodes) {
            if (node != null) node.intersect(rect)?.let {
                intersected.addAll(it)
            }
        }

        return intersected
    }

    fun insert(element: T): Boolean {
        // only* the root needs to check this
        if (depth == 0) {
            if (!bounds.contains(mapper(element))) return false
        }

        if ((objects.size < maxPoints && isLeaf)) {
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

    private fun subdivide() {
        val width = bounds.center.x - bounds.corner.x
        val height = bounds.center.y - bounds.corner.y

        val newDepth = depth + 1

        var node = QuadTree(Rectangle(bounds.corner, width, height), maxPoints, mapper)
        node.depth = newDepth
        nodes[0] = node

        node = QuadTree(Rectangle(Vector2(bounds.center.x, bounds.corner.y), width, height), maxPoints, mapper)
        node.depth = newDepth
        nodes[1] = node

        node = QuadTree(Rectangle(Vector2(bounds.corner.x, bounds.center.y), width, height), maxPoints, mapper)
        node.depth = newDepth
        nodes[2] = node

        node = QuadTree(Rectangle(bounds.center, width, height), maxPoints, mapper)
        node.depth = newDepth
        nodes[3] = node
    }


    fun findNode(v: Vector2): QuadTree<T>? {
        if (!bounds.contains(v)) return null

        if (isLeaf) return this

        for (node in nodes) {
            node?.findNode(v)?.let { return it }
        }

        return null
    }

    fun batch(batchBuilder: RectangleBatchBuilder) {
        batchBuilder.rectangle(bounds)

        for (node in nodes) {
            node?.batch(batchBuilder)
        }
    }

    fun draw(drawer: Drawer) {
        drawer.rectangle(bounds)

        for (node in nodes) {
            node?.draw(drawer)
        }
    }

    override fun toString(): String {
        return "${objects.size}"
    }
}