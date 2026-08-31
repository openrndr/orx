package org.openrndr.extra.bvh

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds

class BVHNode2D(
    var bounds: Rectangle = Rectangle.EMPTY,
    var left: BVHNode2D? = null,
    var right: BVHNode2D? = null,
    var index: Int = -1

) {

    /**
     * Updates the bounding volumes of the nodes by recalculating the bounds based on the provided objects.
     *
     * @param objects The list of objects used to update the bounding volumes of the tree.
     * @param mapper A function that maps an object of type T to its corresponding bounding rectangle.
     * @return The updated bounding rectangle for the current node.
     */

    fun <T> refit(objects: List<T>, mapper: (T) -> Rectangle): Rectangle {
        if (index != -1) {
            bounds = mapper(objects[index])
        } else {
            val leftBounds = left?.refit(objects, mapper) ?: error("no left bounds")
            val rightBounds = right?.refit(objects, mapper) ?: error("no right bounds")
            bounds = listOf(leftBounds, rightBounds).bounds
        }
        return bounds
    }


    /**
     * Queries the tree for all objects that contain the specified point.
     *
     * @param point The point to query within the tree.
     * @param result A mutable list to store the indices of objects containing the point.
     *               If not provided, a new list will be created and returned.
     * @return A list containing the indices of objects that contain the given point.
     */
    fun queryPoint(point: Vector2, result: MutableList<Int> = mutableListOf()): MutableList<Int> {
        if (!bounds.contains(point)) return result

        if (index != -1) {
            result.add(index)
            return result
        }

        left?.queryPoint(point, result)
        right?.queryPoint(point, result)
        return result
    }

    /**
     * Queries the tree for all objects that intersect with the given rectangle.
     *
     * @param rectangle The rectangle used to query intersecting objects.
     * @param result A mutable list that will hold the indices of intersecting objects.
     *               If not provided, a new list will be created and returned.
     * @return A list containing the indices of objects that intersect the given rectangle.
     */
    fun queryRectangle(rectangle: Rectangle, result: MutableList<Int> = mutableListOf()): MutableList<Int> {
        if (!bounds.intersects(rectangle)) {
            return result
        }
        if (index != -1) {
            result.add(index)
            return result
        }
        left?.queryRectangle(rectangle, result)
        right?.queryRectangle(rectangle, result)
        return result

    }

    companion object {

        suspend fun <T> fromObjects(
            objects: List<T>,
            indices: IntArray = IntArray(objects.size) { it },
            mapper: (T) -> Rectangle
        ): BVHNode2D {

            val node = BVHNode2D()


            if (indices.size == 1) {
                node.index = indices[0]
                node.bounds = mapper(objects[indices[0]])
                return node
            }
            val bounds = indices.map {
                mapper(objects[it])
            }.bounds
            node.bounds = bounds

            val axis = if (bounds.width > bounds.height) 0 else 1

            val splitIndex = if (axis == 0) selectNthIndex(indices, indices.size / 2) { mapper(objects[it]).center.x }
            else selectNthIndex(indices, indices.size / 2) { mapper(objects[it]).center.y }


            val splitValue = mapper(objects[indices[splitIndex]]).center[axis]
            val leftIndices = IntArray(indices.size / 2 + 1).apply { fill(-1) }
            val rightIndices = IntArray(indices.size / 2 + 1).apply { fill(-1) }

            var leftCount = 0
            var rightCount = 0
            for (i in indices) {

                if (objects[i].let { mapper(it).center[axis] } < splitValue) {
                    leftIndices[leftCount] = i
                    leftCount++
                } else {
                    rightIndices[rightCount] = i
                    rightCount++
                }
            }


            if (indices.size > 1000) {
                coroutineScope {
                    val right = async {
                        node.right = fromObjects(objects, rightIndices.sliceArray(0..<rightCount), mapper)
                    }
                    val left = async {
                        node.left = fromObjects(objects, leftIndices.sliceArray(0..<leftCount), mapper)
                    }
                    awaitAll(left, right)
                }
            } else {
                node.left = fromObjects( objects, leftIndices.sliceArray(0..<leftCount), mapper)
                node.right = fromObjects( objects, rightIndices.sliceArray(0..<rightCount), mapper)
            }
            return node
        }

    }
}

/**
 * Finds and collects pairs of intersecting leaf-nodes between two bipartite sets
 *
 * @param a The first BVHNode2D tree to check for intersections.
 * @param b The second BVHNode2D tree to check for intersections.
 * @param result A mutable list where intersecting pairs of indices will be added. Defaults to an empty mutable list.
 * @return A list of pairs of indices representing intersecting bounding volumes.
 */
fun findIntersectingPairs(a: BVHNode2D, b: BVHNode2D, result: MutableList<Pair<Int, Int>> = mutableListOf()): List<Pair<Int, Int>> {
    if (!a.bounds.intersects(b.bounds)) {
        return result
    }
    val aLeaf = a.index != -1
    val bLeaf = b.index != -1

    if (aLeaf && bLeaf) {
        result.add(a.index to b.index)
    } else if (aLeaf) {
        findIntersectingPairs(a, b.left!!, result)
        findIntersectingPairs(a, b.right!!, result)
    } else if (bLeaf) {
        findIntersectingPairs(a.left!!, b, result)
        findIntersectingPairs(a.right!!, b, result)
    } else {
        findIntersectingPairs(a.left!!, b.left!!, result)
        findIntersectingPairs(a.left!!, b.right!!, result)
        findIntersectingPairs(a.right!!, b.left!!, result)
        findIntersectingPairs(a.right!!, b.right!!, result)
    }

    return result
}

/**
 * Finds all intersecting pairs of leaf nodes in a 2D Bounding Volume Hierarchy (BVH).
 *
 * This function traverses the provided BVH tree and identifies pairs of leaf nodes
 * whose bounding rectangles intersect. The pairs are returned as a list of index pairs.
 *
 * @param root The root node of the 2D BVH tree to process.
 * @return A list of pairs of integers representing the indices of intersecting leaf nodes in the BVH.
 */
fun findIntersectingPairs(root: BVHNode2D) : List<Pair<Int, Int>> {
    val pairs = mutableListOf<Pair<Int, Int>>()

    fun crossPair(a: BVHNode2D, b: BVHNode2D) {
        if (!a.bounds.intersects(b.bounds)) return

        val aLeaf = a.index != -1
        val bLeaf = b.index != -1

        if (aLeaf && bLeaf) {
            pairs.add(a.index to b.index)
        } else if (aLeaf) {
            crossPair(a, b.left!!)
            crossPair(a, b.right!!)
        } else if (bLeaf) {
            crossPair(a.left!!, b)
            crossPair(a.right!!, b)
        } else {
            crossPair(a.left!!, b.left!!)
            crossPair(a.left!!, b.right!!)
            crossPair(a.right!!, b.left!!)
            crossPair(a.right!!, b.right!!)
        }
    }

    fun selfPair(node: BVHNode2D) {
        if (node.index != -1) {
            return
        }
        selfPair(node.left!!)
        selfPair(node.right!!)
        crossPair(node.left!!, node.right!!)
    }
    selfPair(root)
    return pairs
}