package org.openrndr.extra.kdtree

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.jvm.JvmName


/**
 * Constructs a KD-Tree for a collection of 2D vectors.
 *
 * This function creates a KD-Tree from the given iterable collection of `Vector2` objects.
 * The KD-Tree is built in a way that organizes the points for efficient spatial operations,
 * such as nearest neighbor search or range queries, considering two dimensions (x and y).
 *
 * @return The root node of the KD-Tree representing the input collection of 2D vectors.
 */
@JvmName("kdTreeVector2")
fun Iterable<Vector2>.kdTree(): KDTreeNode<Vector2> {
    val items = this.toMutableList()
    return buildKDTree(items, 2, ::vector2Mapper)
}

/**
 * Constructs a KD-Tree from an iterable collection of 3-dimensional `Vector3` objects.
 *
 * This function converts the input iterable into a mutable list and utilizes the `buildKDTree` method
 * to organize the `Vector3` objects into a spatial data structure for efficient querying.
 *
 * @return The root node of the constructed KD-Tree containing the `Vector3` objects.
 */
@JvmName("kdTreeVector3")
fun Iterable<Vector3>.kdTree(): KDTreeNode<Vector3> {
    val items = this.toMutableList()
    return buildKDTree(items, 3, ::vector3Mapper)
}

/**
 * Constructs a KD-Tree from the iterable collection of 4-dimensional vectors.
 *
 * This method uses the components of each `Vector4` (x, y, z, w) as the coordinate axes
 * in a 4-dimensional space to build the KD-Tree.
 *
 * @receiver An iterable collection of `Vector4` objects to be organized in the KD-Tree.
 * @return The root node of the constructed KD-Tree containing the `Vector4` objects.
 */
@JvmName("kdTreeVector4")
fun Iterable<Vector4>.kdTree(): KDTreeNode<Vector4> {
    val items = this.toMutableList()
    return buildKDTree(items, 4, ::vector4Mapper)
}