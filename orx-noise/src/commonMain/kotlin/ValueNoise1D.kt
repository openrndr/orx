package org.openrndr.extra.noise

fun valueLinear(seed: Int, x: Double) = value(seed, x, ::linear)
fun valueQuintic(seed: Int, x: Double) = value(seed, x, ::quintic)
fun valueHermite(seed: Int, x: Double) = value(seed, x, ::hermite)

inline fun value(seed: Int, x: Double, crossinline interpolation: (Double) -> Double = ::linear): Double =
    value(seed, x, 0.0, interpolation)

val valueLinear1D: (Int, Double) -> Double = ::valueLinear
val valueQuintic1D: (Int, Double) -> Double = ::valueQuintic
val valueHermite1D: (Int, Double) -> Double = ::valueHermite