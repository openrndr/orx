package org.openrndr.extra.noise

fun cubicLinear(seed: Int, x: Double) = cubic(seed, x, ::linear)
fun cubicQuintic(seed: Int, x: Double) = cubic(seed, x, ::quintic)
fun cubicHermite(seed: Int, x: Double) = cubic(seed, x, ::hermite)


fun cubic(seed: Int, x: Double, interpolator: (Double) -> Double = ::linear): Double {
    return cubic(seed, x, 0.0, interpolator)
}

val cubicLinear1D: (Int, Double) -> Double = ::cubicLinear
val cubicQuintic1D: (Int, Double) -> Double = ::cubicQuintic
val cubicHermite1D: (Int, Double) -> Double = ::cubicHermite