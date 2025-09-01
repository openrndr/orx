package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.mix

fun valueLinear(seed: Int, x: Double, y: Double) = value(seed, x, y, ::linear)
fun valueQuintic(seed: Int, x: Double, y: Double) = value(seed, x, y, ::quintic)
fun valueHermite(seed: Int, x: Double, y: Double) = value(seed, x, y, ::hermite)

fun valueLinear(seed: Int, position: Vector2) =
        value(seed, position.x, position.y, ::linear)

fun valueQuintic(seed: Int, position: Vector2) =
        value(seed, position.x, position.y, ::quintic)

fun valueHermite(seed: Int, position: Vector2) =
        value(seed, position.x, position.y, ::hermite)

inline fun value(seed: Int, x: Double, y: Double, crossinline interpolation: (Double) -> Double = ::linear): Double {
    val x0 = x.fastFloor()
    val y0 = y.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val xs = interpolation(x - x0)
    val ys = interpolation(y - y0)

    val xf0 = mix(fshash2D(seed, x0, y0), fshash2D(seed, x1, y0), xs)
    val xf1 = mix(fshash2D(seed, x0, y1), fshash2D(seed, x1, y1), xs)

    return mix(xf0, xf1, ys)
}

val valueLinear2D: (Int, Double, Double) -> Double = ::valueLinear
val valueQuintic2D: (Int, Double, Double) -> Double = ::valueQuintic
val valueHermite2D: (Int, Double, Double) -> Double = ::valueHermite