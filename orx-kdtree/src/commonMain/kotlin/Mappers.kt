package org.openrndr.extra.kdtree

import org.openrndr.math.Vector2
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4


/**
 * Maps a 2D vector's dimension to its corresponding value.
 *
 * @param v The 2D vector whose dimension is to be mapped.
 * @param dimension The dimension index to map (0 for x, any other value for y).
 * @return The value of the specified dimension of the vector.
 */
fun vector2Mapper(v: Vector2, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        else -> v.y
    }
}

/**
 * Maps the specified dimension of an IntVector2 to a Double.
 *
 * @param v the IntVector2 instance containing integer components x and y.
 * @param dimension the dimension to map (0 for x, any other value for y).
 * @return the x or y component of the vector as a Double, depending on the specified dimension.
 */
fun intVector2Mapper(v: IntVector2, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x.toDouble()
        else -> v.y.toDouble()
    }
}

/**
 * Maps a Vector3 object to one of its components (x, y, or z) based on the specified dimension.
 *
 * @param v the Vector3 object whose component is to be retrieved
 * @param dimension the index representing the component to be retrieved (0 for x, 1 for y, others for z)
 * @return the component value corresponding to the specified dimension
 */
fun vector3Mapper(v: Vector3, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        1 -> v.y
        else -> v.z
    }
}

/**
 * Maps the components of a 4-dimensional vector based on the specified dimension index.
 *
 * @param v the 4-dimensional vector containing the components x, y, z, and w
 * @param dimension the index of the dimension to retrieve; 0 for x, 1 for y, 2 for z, and any other value for w
 * @return the value of the vector component corresponding to the specified dimension index
 */
fun vector4Mapper(v: Vector4, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        1 -> v.y
        2 -> v.z
        else -> v.w
    }
}

