package org.openrndr.extra.noise

fun hermite(t: Double): Double {
    return t * t * (3 - 2 * t)
}

fun quintic(t: Double): Double {
    return t * t * t * (t * (t * 6 - 15) + 10)
}

fun cubic(a: Double, b: Double, c: Double, d: Double, t: Double): Double {
    val p = d - c - (a - b)
    return t * t * t * p + t * t * (a - b - p) + t * (c - a) + b
}

fun linear(x: Double): Double {
    return x
}