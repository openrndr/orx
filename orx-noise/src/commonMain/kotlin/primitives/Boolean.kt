package org.openrndr.extra.noise.primitives

import kotlin.random.Random

/**
 * Generates a random Boolean value based on the provided probability.
 *
 * @param probability The probability of returning `true`. Must be a value between 0.0 and 1.0. Default is 0.5.
 * @param random An instance of `Random` used to generate the random value. Default is `Random.Default`.
 * @return A Boolean value, which is `true` with the specified probability.
 */
fun Boolean.Companion.random(probability: Double = 0.5, random: Random = Random.Default) =
    random.nextDouble() < probability


/**
 * Generates a list of random boolean values based on the specified probability.
 *
 * @param count Number of random boolean values to generate.
 * @param probability Probability of generating `true` for each boolean value. Default is 0.5.
 * @param random Instance of `Random` to use for generating random values. Default is `Random.Default`.
 * @return A list of randomly generated boolean values.
 */
fun Boolean.Companion.randoms(count: Int, probability: Double = 0.5, random: Random = Random.Default) =
    List(count) { random.nextDouble() < probability }
