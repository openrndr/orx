package org.openrndr.extra.processing

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import processing.core.PVector

/**
 * Converts a given `Vector2` instance into a `PVector` instance.
 *
 * @param v the source Vector2 whose x and y coordinates will be used to create the PVector.
 * @return a new PVector instance initialized with the x and y components of the given Vector2,
 * cast to Float.
 */
fun PVector(v: Vector2): PVector = PVector(v.x.toFloat(), v.y.toFloat())

/**
 * Converts a `Vector3` object into a `PVector` instance by converting its components to `Float`.
 *
 * @param v the `Vector3` instance to convert
 * @return a `PVector` instance with corresponding x, y, and z components in `Float`
 */
fun PVector(v: Vector3): PVector = PVector(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())

/**
 * Converts an instance of [Vector2] to a [PVector] by transforming its x and y values
 * into floating-point numbers.
 *
 * @receiver The [Vector2] instance to be converted.
 * @return A new [PVector] containing the x and y components of the receiver as floats.
 */
fun Vector2.toPVector() = PVector(this.x.toFloat(), this.y.toFloat())

/**
 * Converts a [Vector3] instance to a [PVector] instance.
 *
 * Each component of the [Vector3] (x, y, z) is cast to a float and used to
 * construct a new [PVector].
 *
 * @receiver The [Vector3] to be converted.
 * @return A [PVector] with the corresponding float components.
 */
fun Vector3.toPVector() = PVector(this.x.toFloat(), this.y.toFloat(), this.z.toFloat())


/**
 * Converts this [PVector] instance into a [Vector2] instance.
 *
 * The `x` and `y` components of the [PVector] are converted to `Double` and used
 * to create a new [Vector2].
 *
 * @return A [Vector2] instance with the `x` and `y` components of this [PVector]
 * converted to `Double`.
 */
fun PVector.toVector2(): Vector2 {
    return Vector2(x.toDouble(), y.toDouble())
}

/**
 * Converts a [PVector] into an [Vector3] instance.
 *
 * @return a [Vector3] object with the same x, y, and z values as the original [PVector],
 * converted to Double.
 */
fun PVector.toVector3(): Vector3 {
    return Vector3(x.toDouble(), y.toDouble(), z.toDouble())
}
