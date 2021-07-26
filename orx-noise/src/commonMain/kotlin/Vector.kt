package org.openrndr.extra.noise

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import kotlin.jvm.JvmName

@JvmName("polarWithVector2Output")
fun ((Int, Polar) -> Double).withVector2Output(): (Int, Polar) -> Vector2 =
    { seed, polar -> Vector2(this(seed, polar), this(seed xor 0x7f7f7f7f, Polar(-polar.theta, polar.radius))) }

fun ((Int, Double) -> Double).withVector2Output(): (seed: Int, x: Double) -> Vector2 =
    { seed: Int, x: Double -> Vector2(this(seed, x), this(seed xor 0x7f7f7f7f, -x)) }

fun ((Int, Double, Double) -> Double).withVector2Output(): (seed: Int, x: Double, y: Double) -> Vector2 =
    { seed, x, y -> Vector2(this(seed, x, y), this(seed xor 0x7f7f7f7f, y, -x)) }

fun ((Int, Double, Double, Double) -> Double).withVector2Output(): (seed: Int, x: Double, y: Double, z: Double) -> Vector2 =
    { seed, x, y, z -> Vector2(this(seed, x, y, z), this(seed xor 0x7f7f7f7f, y, -x, z)) }

