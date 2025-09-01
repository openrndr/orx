package org.openrndr.extra.noise.linearrange

import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.math.linearrange.*
import org.openrndr.math.LinearType
import kotlin.random.Random

/**
 * Generates a uniformly distributed random value within the range.
 *
 * @param random The random number generator to use for generating the value. Defaults to [Random.Default].
 * @return A value of type [T] sampled uniformly within the range.
 */
fun <T : LinearType<T>> LinearRange1D<T>.uniform(random: Random = Random.Default): T = value(random.nextDouble())

/**
 * Generates a random value within the 2D linear range based on a uniform distribution.
 *
 * @param random The random number generator to use for producing random values, defaults to Random.Default.
 * @return A randomly generated value of type T within the linear range.
 */
fun <T : LinearType<T>> LinearRange2D<T>.uniform(random: Random = Random.Default): T =
    value(random.nextDouble(), random.nextDouble())

/**
 * Generates a uniform random value within the 3D linear range, based on the given random number generator.
 *
 * @param random The random number generator to use for generating random values. Defaults to `Random.Default`.
 * @return A randomly generated value of type `T` within the 3D linear range.
 */
fun <T : LinearType<T>> LinearRange3D<T>.uniform(random: Random = Random.Default): T =
    value(random.nextDouble(), random.nextDouble(), random.nextDouble())

/**
 * Generates a value of type `T` uniformly distributed within the 4D linear range.
 *
 * @param random The random number generator to use. Defaults to `Random.Default`.
 * @return A uniformly distributed value of type `T` within the 4D range.
 */
fun <T : LinearType<T>> LinearRange4D<T>.uniform(random: Random = Random.Default): T =
    value(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble())


/**
 * Computes a hashed value based on the provided seed and input, and generates
 * a `LinearType` instance using the hash results.
 *
 * @param seed The seed value used for hash computation.
 * @param x The integer input value used for hash computation.
 * @return a `LinearType` instance computed from the hash values
 *
 */
fun <T : LinearType<T>> LinearRange1D<T>.hash(seed: Int, x: Int) : T = value(fhash1D(seed, x))

/**
 * Computes a hashed value based on the provided seed and input, and generates
 * a `LinearType` instance using the hash results.
 *
 * @param seed an integer seed value used to initialize the hash computation
 * @param x an integer input used in the hash computation
 * @return a `LinearType` instance computed from the hash values
 */
fun <T : LinearType<T>> LinearRange2D<T>.hash(seed: Int, x: Int) : T =
    value(fhash1D(seed, x), fhash1D(seed xor 0x7f7f_7f7f, x))

/**
 * Computes a hashed value based on the provided seed and input, and generates
 * a `LinearType` instance using the hash results.
 *
 * @param seed an integer seed value used to initialize the hash computation
 * @param x an integer input used in the hash computation
 * @return a `LinearType` instance computed from the hash values
 */
fun <T : LinearType<T>> LinearRange3D<T>.hash(seed: Int, x: Int) :T {
    val x4 = x * 3
    return value(fhash1D(seed, x4), fhash1D(seed, x4 + 1),  fhash1D(seed, x4 + 2))
}

/**
 * Computes a hashed value based on the provided seed and input, and generates
 * a `LinearType` instance using the hash results.
 *
 * @param seed an integer seed value used to initialize the hash computation
 * @param x an integer input used in the hash computation
 * @return a `LinearType` instance computed from the hash values
 */
fun <T : LinearType<T>> LinearRange4D<T>.hash(seed: Int, x: Int) {
    val x4 = x * 4
    value(fhash1D(seed, x4), fhash1D(seed, x4 + 1),  fhash1D(seed, x4 + 2), fhash1D(seed, x4 + 3))
}