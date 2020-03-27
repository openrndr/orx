package org.openrndr.extra.noise

fun valueLinear(seed: Int, x: Double) = value(seed, x, ::linear)
fun valueQuintic(seed: Int, x: Double) = value(seed, x, ::quintic)
fun valueHermite(seed: Int, x: Double) = value(seed, x, ::hermite)

inline fun value(seed: Int, x: Double, @Suppress("UNUSED_PARAMETER") crossinline interpolation: (Double) -> Double = ::linear): Double =
        value(seed, x, 0.0)