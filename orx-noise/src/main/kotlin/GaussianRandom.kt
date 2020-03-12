package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.random.Random

fun gaussian(mean: Double = 0.0, deviation: Double = 1.0, random: Random = Random.Default): Double {
    var v1: Double
    var v2: Double
    var s: Double
    do {
        v1 = 2 * random.nextDouble() - 1
        v2 = 2 * random.nextDouble() - 1
        s = v1 * v1 + v2 * v2
    } while (s >= 1 || s == 0.0)
    val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)

    return v1 * multiplier * deviation + mean
}

fun Double.Companion.gaussian(mean: Double = 0.0, deviation: Double = 1.0, random: Random = Random.Default): Double {
    return gaussian(mean, deviation, random)
}

fun Vector2.Companion.gaussian(mean: Vector2 = Vector2.ZERO, deviation: Vector2 = Vector2.ONE, random: Random = Random.Default): Vector2 {
    return Vector2(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random))
}

fun Vector3.Companion.gaussian(mean: Vector3 = Vector3.ZERO, deviation: Vector3 = Vector3.ONE, random: Random = Random.Default): Vector3 {
    return Vector3(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random), gaussian(mean.z, deviation.z, random))
}

fun Vector4.Companion.gaussian(mean: Vector4 = Vector4.ZERO, deviation: Vector4 = Vector4.ONE, random: Random = Random.Default): Vector4 {
    return Vector4(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random), gaussian(mean.z, deviation.z, random), gaussian(mean.w, deviation.w, random))
}

