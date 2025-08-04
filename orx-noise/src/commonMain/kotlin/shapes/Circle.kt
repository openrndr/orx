package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.fhash1D
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates a random point within the bounds of the `Circle` using a hash-based approach.
 *
 * @param seed An integer seed for the hash function, used to produce deterministic random results for the same seed.
 * @param x An integer input to the hash function, adding further variation to the generated point.
 * @return A `Vector2` representing a random point within the `Circle`, based on the provided `seed` and `x`.
 */
fun Circle.hash(seed: Int, x: Int): Vector2 {
    val r = radius * sqrt(fhash1D(seed, x))
    val phi = 360.0 * fhash1D(seed xor 0x7f7f_7f7f, x)
    return Polar(phi, r).cartesian + center
}

/**
 * Generates a uniformly distributed random point within the `Circle`.
 *
 * @param random An optional random number generator to use. Defaults to `Random.Default`.
 * @return A `Vector2` representing a random point within the `Circle`.
 */
fun Circle.uniform(random: Random = Random.Default): Vector2 {
    val r = radius * sqrt(random.nextDouble())
    val phi = 360.0 * random.nextDouble()
    return Polar(phi, r).cartesian + center
}