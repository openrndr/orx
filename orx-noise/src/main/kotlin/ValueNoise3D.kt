package org.openrndr.extra.noise

import org.openrndr.math.Vector3

fun valueLinear(seed: Int, x: Double, y: Double, z: Double) = value(seed, x, y, z, ::linear)
fun valueQuintic(seed: Int, x: Double, y: Double, z: Double) = value(seed, x, y, z, ::quintic)
fun valueHermite(seed: Int, x: Double, y: Double, z: Double) = value(seed, x, y, z, ::hermite)

fun valueLinear(seed: Int, position: Vector3) =
        value(seed, position.x, position.y, position.z, ::linear)

fun valueQuintic(seed: Int, position: Vector3) =
        value(seed, position.x, position.y, position.z, ::quintic)

fun valueHermite(seed: Int, position: Vector3) =
        value(seed, position.x, position.y, position.z, ::hermite)

inline fun value(seed: Int, x: Double, y: Double, z: Double, crossinline interpolation: (Double) -> Double = ::linear): Double {
    val x0 = x.fastFloor()
    val y0 = y.fastFloor()
    val z0 = z.fastFloor()
    val x1 = x0 + 1
    val y1 = y0 + 1
    val z1 = z0 + 1

    val xs = interpolation(x - x0)
    val ys = interpolation(y - y0)
    val zs = interpolation(z - z0)


    val xf00 = lerp(valCoord3D(seed, x0, y0, z0), valCoord3D(seed, x1, y0, z0), xs)
    val xf10 = lerp(valCoord3D(seed, x0, y1, z0), valCoord3D(seed, x1, y1, z0), xs)
    val xf01 = lerp(valCoord3D(seed, x0, y0, z1), valCoord3D(seed, x1, y0, z1), xs)
    val xf11 = lerp(valCoord3D(seed, x0, y1, z1), valCoord3D(seed, x1, y1, z1), xs)

    val yf0 = lerp(xf00, xf10, ys)
    val yf1 = lerp(xf01, xf11, ys)

    return lerp(yf0, yf1, zs)
}