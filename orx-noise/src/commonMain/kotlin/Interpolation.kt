package org.openrndr.extra.noise

/**
 * Computes the Hermite interpolation function value for the given parameter.
 *
 * The Hermite interpolation ensures smooth transitions and is often used in
 * animations or procedural generation to create a smooth curve between points.
 *
 * @param t A double value representing the parameter for the interpolation.
 *          It is usually expected to be in the range [0.0, 1.0].
 * @return The interpolated value based on the Hermite polynomial.
 */
fun hermite(t: Double): Double {
    return t * t * (3 - 2 * t)
}

/**
 * Calculates the result of the quintic polynomial function, commonly used in smooth interpolation.
 *
 * The function is defined as t^3 * (t * (t * 6 - 15) + 10), where t is the input value.
 *
 * @param t The input value for which the quintic function is to be calculated.
 * @return The result of the quintic polynomial for the given input value.
 */
fun quintic(t: Double): Double {
    return t * t * t * (t * (t * 6 - 15) + 10)
}

/**
 * Computes the value of a cubic interpolation given control points and a parameter t.
 *
 * @param a The first control point.
 * @param b The second control point.
 * @param c The third control point.
 * @param d The fourth control point.
 * @param t The interpolation factor, typically between 0 and 1.
 * @return The interpolated value at the given t.
 */
fun cubic(a: Double, b: Double, c: Double, d: Double, t: Double): Double {
    val p = d - c - (a - b)
    return t * t * t * p + t * t * (a - b - p) + t * (c - a) + b
}

fun linear(x: Double): Double {
    return x
}