package org.openrndr.extra.noise

import org.openrndr.math.Vector2

fun perlin(seed: Int, x: Double, y: Double) = perlin(seed, x, y, ::linear)
fun perlinLinear(seed: Int, x: Double, y: Double) = perlin(seed, x, y, ::linear)
fun perlinQuintic(seed: Int, x: Double, y: Double) = perlin(seed, x, y, ::quintic)
fun perlinHermite(seed: Int, x: Double, y: Double) = perlin(seed, x, y, ::hermite)

fun perlin(seed: Int, position: Vector2) = perlin(seed, position.x, position.y, ::linear)
fun perlinLinear(seed: Int, position: Vector2) = perlin(seed, position.x, position.y, ::linear)
fun perlinQuintic(seed: Int, position: Vector2) = perlin(seed, position.x, position.y, ::quintic)
fun perlinHermite(seed: Int, position: Vector2) = perlin(seed, position.x, position.y, ::hermite)

inline fun perlin(seed: Int, x: Double, y: Double, crossinline interpolator: (Double) -> Double): Double {
    val x0 = x.fastFloor()
    val y0 = y.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val xs = interpolator(x - x0)
    val ys = interpolator(y - y0)

    val xd0 = x - x0
    val yd0 = y - y0
    val xd1 = xd0 - 1
    val yd1 = yd0 - 1

    val xf0 = lerp(gradCoord2D(seed, x0, y0, xd0, yd0), gradCoord2D(seed, x1, y0, xd1, yd0), xs)
    val xf1 = lerp(gradCoord2D(seed, x0, y1, xd0, yd1), gradCoord2D(seed, x1, y1, xd1, yd1), xs)

    return lerp(xf0, xf1, ys)
}