package org.openrndr.extra.noise

fun cubicLinear(seed: Int, x: Double, y: Double) = cubic(seed, x, y, ::linear)
fun cubicQuintic(seed: Int, x: Double, y: Double) = cubic(seed, x, y, ::quintic)
fun cubicHermite(seed: Int, x: Double, y: Double) = cubic(seed, x, y, ::hermite)

private const val CUBIC_2D_BOUNDING = 1 / (1.5 * 1.5).toFloat()
fun cubic(seed: Int, x: Double, y: Double, interpolator: (Double) -> Double = ::linear): Double {
    val x1 = x.fastFloor()
    val y1 = y.fastFloor()

    val x0 = x1 - 1
    val y0 = y1 - 1
    val x2 = x1 + 1
    val y2 = y1 + 1
    val x3 = x1 + 2
    val y3 = y1 + 2

    val xs = interpolator(x - x1.toDouble())
    val ys = interpolator(y - y1.toDouble())

    return cubic(
            cubic(valCoord2D(seed, x0, y0), valCoord2D(seed, x1, y0), valCoord2D(seed, x2, y0), valCoord2D(seed, x3, y0),
                    xs),
            cubic(valCoord2D(seed, x0, y1), valCoord2D(seed, x1, y1), valCoord2D(seed, x2, y1), valCoord2D(seed, x3, y1),
                    xs),
            cubic(valCoord2D(seed, x0, y2), valCoord2D(seed, x1, y2), valCoord2D(seed, x2, y2), valCoord2D(seed, x3, y2),
                    xs),
            cubic(valCoord2D(seed, x0, y3), valCoord2D(seed, x1, y3), valCoord2D(seed, x2, y3), valCoord2D(seed, x3, y3),
                    xs),
            ys) * CUBIC_2D_BOUNDING
}