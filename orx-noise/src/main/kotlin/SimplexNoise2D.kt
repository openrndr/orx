package org.openrndr.extra.noise

private const val G2 = 1.0 / 4.0
private const val F2 = 1.0 / 2.0

fun simplex(seed: Int, x: Double, y: Double): Double {
    var t = (x + y) * F2
    val i = (x + t).fastFloor()
    val j = (y + t).fastFloor()

    t = ((i + j) * G2)
    val X0 = i - t
    val Y0 = j - t

    val x0 = x - X0
    val y0 = y - Y0

    val i1: Int
    val j1: Int
    if (x0 > y0) {
        i1 = 1
        j1 = 0
    } else {
        i1 = 0
        j1 = 1
    }

    val x1 = (x0 - i1 + G2)
    val y1 = (y0 - j1 + G2)
    val x2 = (x0 - 1 + F2)
    val y2 = (y0 - 1 + F2)

    val n0: Double
    val n1: Double
    val n2: Double

    t = 0.5 - x0 * x0 - y0 * y0
    if (t < 0)
        n0 = 0.0
    else {
        t *= t
        n0 = t * t * gradCoord2D(seed, i, j, x0, y0)
    }

    t = 0.5 - x1 * x1 - y1 * y1
    if (t < 0)
        n1 = 0.0
    else {
        t *= t
        n1 = t * t * gradCoord2D(seed, i + i1, j + j1, x1, y1)
    }

    t = 0.5 - x2 * x2 - y2 * y2
    if (t < 0)
        n2 = 0.0
    else {
        t *= t
        n2 = t * t * gradCoord2D(seed, i + 1, j + 1, x2, y2)
    }

    return 50.0 * (n0 + n1 + n2)
}