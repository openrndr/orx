package org.openrndr.extra.noise

fun perlin(seed: Int, x: Double) = perlin(seed, x, ::linear)
fun perlinLinear(seed: Int, x: Double) = perlin(seed, x, ::linear)
fun perlinQuintic(seed: Int, x: Double) = perlin(seed, x, ::quintic)
fun perlinHermite(seed: Int, x: Double) = perlin(seed, x, ::hermite)

inline fun perlin(seed: Int, x: Double, crossinline interpolator: (Double) -> Double): Double =
    perlin(seed, x, 0.0, interpolator)

val perlin1D: (Int, Double) -> Double = ::perlin