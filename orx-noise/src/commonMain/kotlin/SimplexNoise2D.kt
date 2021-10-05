package org.openrndr.extra.noise

import org.openrndr.math.Vector2

private const val SQRT3 = 1.7320508075688772935274463415059
private const val F2 = 0.5 * (SQRT3 - 1.0)
private const val G2 = (3.0 - SQRT3) / 6.0

fun simplex(seed: Int, position: Vector2): Double = simplex(seed, position.x, position.y)


//fun simplex2D(seed:Int, x:Double, y:Double) = simplex(seed, x, y)
fun simplex(seed: Int, x: Double, y: Double): Double {
    var t = (x + y) * F2
    val i = (x + t).fastFloor()
    val j = (y + t).fastFloor()

    t = ((i + j) * G2)
    val x0 = x - (i - t)
    val y0 = y - (j - t)

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
    val x2 = (x0 - 1 + 2 * G2)
    val y2 = (y0 - 1 + 2 * G2)

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

val simplex1D: (Int, Double) -> Double = ::simplex
val simplex2D: (Int, Double, Double) -> Double = ::simplex
val simplex3D: (Int, Double, Double, Double) -> Double = ::simplex
val simplex4D: (Int, Double, Double, Double) -> Double = ::simplex

fun Vector2.Companion.simplex(seed: Int, x: Double): Vector2 = Vector2(
    simplex(seed, x, 0.0, 0.0, 0.0),
    simplex(seed, 0.0, x + 31.3383, 0.0, 0.0)
)