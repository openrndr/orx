package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates a random number following a Gaussian (normal) distribution.
 *
 * @param mean The mean of the Gaussian distribution. Defaults to 0.0.
 * @param deviation The standard deviation of the Gaussian distribution. Defaults to 1.0.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A random number sampled from the specified Gaussian distribution.
 */
fun gaussian(mean: Double = 0.0, deviation: Double = 1.0, random: Random = Random.Default): Double {
    var v1: Double
    var v2: Double
    var s: Double
    do {
        v1 = 2 * random.nextDouble() - 1
        v2 = 2 * random.nextDouble() - 1
        s = v1 * v1 + v2 * v2
    } while (s >= 1 || s == 0.0)
    val multiplier = sqrt(-2 * ln(s) / s)

    return v1 * multiplier * deviation + mean
}

/**
 * Generates a random number following a Gaussian (normal) distribution.
 *
 * @param mean The mean of the Gaussian distribution. Defaults to 0.0.
 * @param deviation The standard deviation of the Gaussian distribution. Defaults to 1.0.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A random number sampled from the specified Gaussian distribution.
 */
fun Double.Companion.gaussian(
    mean: Double = 0.0,
    deviation: Double = 1.0,
    random: Random = Random.Default
): Double = org.openrndr.extra.noise.gaussian(mean, deviation, random)

/**
 * Generates a random 2D vector with components sampled from independent Gaussian (normal) distributions.
 *
 * @param mean The mean vector of the Gaussian distributions for the x and y components. Defaults to Vector2.ZERO.
 * @param deviation The standard deviation vector of the Gaussian distributions for the x and y components. Defaults to Vector2.ONE.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A 2D vector with components sampled from their respective Gaussian distributions.
 */
fun Vector2.Companion.gaussian(mean: Vector2 = Vector2.ZERO, deviation: Vector2 = Vector2.ONE, random: Random = Random.Default): Vector2 {
    return Vector2(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random))
}

/**
 * Generates a random Vector3 following a Gaussian (normal) distribution.
 *
 * @param mean The mean vector for the Gaussian distribution. Defaults to Vector3.ZERO.
 * @param deviation The standard deviation vector for the Gaussian distribution. Defaults to Vector3.ONE.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A random Vector3 sampled from the specified Gaussian distribution.
 */
fun Vector3.Companion.gaussian(mean: Vector3 = Vector3.ZERO, deviation: Vector3 = Vector3.ONE, random: Random = Random.Default): Vector3 {
    return Vector3(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random), gaussian(mean.z, deviation.z, random))
}

/**
 * Generates a random `Vector4` where each component is sampled independently from a Gaussian (normal) distribution.
 *
 * @param mean A `Vector4` representing the mean of the distribution for each component. Defaults to `Vector4.ZERO`.
 * @param deviation A `Vector4` representing the standard deviation of the distribution for each component. Defaults to `Vector4.ONE`.
 * @param random The random number generator to use. Defaults to `Random.Default`.
 * @return A `Vector4` where each component is a random number sampled from the specified Gaussian distribution.
 */
fun Vector4.Companion.gaussian(mean: Vector4 = Vector4.ZERO, deviation: Vector4 = Vector4.ONE, random: Random = Random.Default): Vector4 {
    return Vector4(gaussian(mean.x, deviation.x, random), gaussian(mean.y, deviation.y, random), gaussian(mean.z, deviation.z, random), gaussian(mean.w, deviation.w, random))
}

