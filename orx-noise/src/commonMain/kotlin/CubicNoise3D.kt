package org.openrndr.extra.noise

import org.openrndr.math.Vector3

private const val CUBIC_3D_BOUNDING = 1 / (1.5 * 1.5 * 1.5).toFloat()

fun cubic(seed: Int, x: Double, y: Double, z: Double) = cubic(seed, x, y, z, ::linear)
fun cubicLinear(seed: Int, x: Double, y: Double, z: Double) = cubic(seed, x, y, z, ::linear)
fun cubicQuintic(seed: Int, x: Double, y: Double, z: Double) = cubic(seed, x, y, z, ::quintic)
fun cubicHermite(seed: Int, x: Double, y: Double, z: Double) = perlin(seed, x, y, z, ::hermite)

fun cubic(seed: Int, position: Vector3) = cubic(seed, position.x, position.y, position.z, ::linear)
fun cubicLinear(seed: Int, position: Vector3) = cubic(seed, position.x, position.y, position.z, ::linear)
fun cubicQuintic(seed: Int, position: Vector3) = cubic(seed, position.x, position.y, position.z, ::quintic)
fun cubicHermite(seed: Int, position: Vector3) = perlin(seed, position.x, position.y, position.z, ::hermite)

fun cubic(seed: Int, x: Double, y: Double, z: Double, interpolator: (Double) -> Double): Double {
    val x1 = x.fastFloor()
    val y1 = y.fastFloor()
    val z1 = z.fastFloor()

    val x0 = x1 - 1
    val y0 = y1 - 1
    val z0 = z1 - 1
    val x2 = x1 + 1
    val y2 = y1 + 1
    val z2 = z1 + 1
    val x3 = x1 + 2
    val y3 = y1 + 2
    val z3 = z1 + 2

    val xs = interpolator(x - x1.toFloat())
    val ys = interpolator(y - y1.toFloat())
    val zs = interpolator(z - z1.toFloat())

    return cubic(
            cubic(
                    cubic(valCoord3D(seed, x0, y0, z0), valCoord3D(seed, x1, y0, z0), valCoord3D(seed, x2, y0, z0), valCoord3D(seed, x3, y0, z0), xs),
                    cubic(valCoord3D(seed, x0, y1, z0), valCoord3D(seed, x1, y1, z0), valCoord3D(seed, x2, y1, z0), valCoord3D(seed, x3, y1, z0), xs),
                    cubic(valCoord3D(seed, x0, y2, z0), valCoord3D(seed, x1, y2, z0), valCoord3D(seed, x2, y2, z0), valCoord3D(seed, x3, y2, z0), xs),
                    cubic(valCoord3D(seed, x0, y3, z0), valCoord3D(seed, x1, y3, z0), valCoord3D(seed, x2, y3, z0), valCoord3D(seed, x3, y3, z0), xs),
                    ys),
            cubic(
                    cubic(valCoord3D(seed, x0, y0, z1), valCoord3D(seed, x1, y0, z1), valCoord3D(seed, x2, y0, z1), valCoord3D(seed, x3, y0, z1), xs),
                    cubic(valCoord3D(seed, x0, y1, z1), valCoord3D(seed, x1, y1, z1), valCoord3D(seed, x2, y1, z1), valCoord3D(seed, x3, y1, z1), xs),
                    cubic(valCoord3D(seed, x0, y2, z1), valCoord3D(seed, x1, y2, z1), valCoord3D(seed, x2, y2, z1), valCoord3D(seed, x3, y2, z1), xs),
                    cubic(valCoord3D(seed, x0, y3, z1), valCoord3D(seed, x1, y3, z1), valCoord3D(seed, x2, y3, z1), valCoord3D(seed, x3, y3, z1), xs),
                    ys),
            cubic(
                    cubic(valCoord3D(seed, x0, y0, z2), valCoord3D(seed, x1, y0, z2), valCoord3D(seed, x2, y0, z2), valCoord3D(seed, x3, y0, z2), xs),
                    cubic(valCoord3D(seed, x0, y1, z2), valCoord3D(seed, x1, y1, z2), valCoord3D(seed, x2, y1, z2), valCoord3D(seed, x3, y1, z2), xs),
                    cubic(valCoord3D(seed, x0, y2, z2), valCoord3D(seed, x1, y2, z2), valCoord3D(seed, x2, y2, z2), valCoord3D(seed, x3, y2, z2), xs),
                    cubic(valCoord3D(seed, x0, y3, z2), valCoord3D(seed, x1, y3, z2), valCoord3D(seed, x2, y3, z2), valCoord3D(seed, x3, y3, z2), xs),
                    ys),
            cubic(
                    cubic(valCoord3D(seed, x0, y0, z3), valCoord3D(seed, x1, y0, z3), valCoord3D(seed, x2, y0, z3), valCoord3D(seed, x3, y0, z3), xs),
                    cubic(valCoord3D(seed, x0, y1, z3), valCoord3D(seed, x1, y1, z3), valCoord3D(seed, x2, y1, z3), valCoord3D(seed, x3, y1, z3), xs),
                    cubic(valCoord3D(seed, x0, y2, z3), valCoord3D(seed, x1, y2, z3), valCoord3D(seed, x2, y2, z3), valCoord3D(seed, x3, y2, z3), xs),
                    cubic(valCoord3D(seed, x0, y3, z3), valCoord3D(seed, x1, y3, z3), valCoord3D(seed, x2, y3, z3), valCoord3D(seed, x3, y3, z3), xs),
                    ys),
            zs) * CUBIC_3D_BOUNDING
}

val cubicLinear3D: (Int, Double, Double, Double) -> Double = ::cubicLinear
val cubicQuintic3D: (Int, Double, Double, Double) -> Double = ::cubicQuintic
val cubicHermite3D: (Int, Double, Double, Double) -> Double = ::cubicHermite