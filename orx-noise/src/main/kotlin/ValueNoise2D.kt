package org.openrndr.extra.noise

fun valueLinear(seed: Int, x: Double, y: Double) = value(seed, x, y, ::linear)
fun valueQuintic(seed: Int, x: Double, y: Double) = value(seed, x, y, ::quintic)
fun valueHermite(seed: Int, x: Double, y: Double) = value(seed, x, y, ::hermite)

inline fun value(seed: Int, x: Double, y: Double, crossinline interpolation: (Double) -> Double = ::linear): Double {
    val x0 = x.fastFloor()
    val y0 = y.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val xs = interpolation(x - x0)
    val ys = interpolation(y - y0)

    val xf0 = lerp(valCoord2D(seed, x0, y0), valCoord2D(seed, x1, y0), xs)
    val xf1 = lerp(valCoord2D(seed, x0, y1), valCoord2D(seed, x1, y1), xs)

    return lerp(xf0, xf1, ys)
}