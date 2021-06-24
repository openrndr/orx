package org.openrndr.extra.noise

import org.openrndr.math.Vector3
import org.openrndr.math.mix

fun perlinLinear(seed: Int, x: Double, y: Double, z: Double) = perlin(seed, x, y, z, ::linear)
fun perlinQuintic(seed: Int, x: Double, y: Double, z: Double) = perlin(seed, x, y, z, ::quintic)
fun perlinHermite(seed: Int, x: Double, y: Double, z: Double) = perlin(seed, x, y, z, ::hermite)

fun perlinLinear(seed: Int, position: Vector3) = perlin(seed, position.x, position.y, position.z, ::linear)
fun perlinQuintic(seed: Int, position: Vector3) = perlin(seed, position.x, position.y, position.z, ::quintic)
fun perlinHermite(seed: Int, position: Vector3) = perlin(seed, position.x, position.y, position.z, ::hermite)

inline fun perlin(seed: Int, x: Double, y: Double, z: Double, crossinline interpolator: (Double) -> Double = ::linear): Double {
    val x0 = x.fastFloor()
    val y0 = y.fastFloor()
    val z0 = z.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1
    val z1 = z0 + 1

    val xs: Double = interpolator(x - x0)
    val ys: Double = interpolator(y - y0)
    val zs: Double = interpolator(z - z0)

    val xd0 = x - x0
    val yd0 = y - y0
    val zd0 = z - z0
    val xd1 = xd0 - 1
    val yd1 = yd0 - 1
    val zd1 = zd0 - 1

    val xf00 = mix(gradCoord3D(seed, x0, y0, z0, xd0, yd0, zd0), gradCoord3D(seed, x1, y0, z0, xd1, yd0, zd0), xs)
    val xf10 = mix(gradCoord3D(seed, x0, y1, z0, xd0, yd1, zd0), gradCoord3D(seed, x1, y1, z0, xd1, yd1, zd0), xs)
    val xf01 = mix(gradCoord3D(seed, x0, y0, z1, xd0, yd0, zd1), gradCoord3D(seed, x1, y0, z1, xd1, yd0, zd1), xs)
    val xf11 = mix(gradCoord3D(seed, x0, y1, z1, xd0, yd1, zd1), gradCoord3D(seed, x1, y1, z1, xd1, yd1, zd1), xs)

    val yf0 = mix(xf00, xf10, ys)
    val yf1 = mix(xf01, xf11, ys)

    return mix(yf0, yf1, zs)
}