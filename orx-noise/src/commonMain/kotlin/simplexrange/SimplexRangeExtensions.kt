package org.openrndr.extra.noise.simplexrange

import org.openrndr.math.LinearType
import org.openrndr.extra.math.simplexrange.SimplexRange4D
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import org.openrndr.extra.math.simplexrange.SimplexRange2D
import kotlin.random.Random

/**
 * Generates a uniformly distributed value within the SimplexRange2D.
 *
 * @param random the random number generator used to produce random values.
 * @return a value of type T sampled uniformly within the 2D simplex range.
 */
fun <T:LinearType<T>> SimplexRange2D<T>.uniform(random: Random): T {
    return value(random.nextDouble(), random.nextDouble())
}

/**
 * Generates a uniformly distributed value within the 3D simplex range.
 *
 * @param random the random number generator to produce the distribution.
 * @return a value of type [T] sampled uniformly within the range.
 */
fun <T:LinearType<T>> SimplexRange3D<T>.uniform(random: Random): T {
    return value(random.nextDouble(), random.nextDouble(), random.nextDouble())
}

/**
 * Generates a uniformly distributed value within the 4D simplex range using a random generator.
 *
 * @param random an instance of the random number generator used to produce random values
 * @return a value of type `T` that represents a point within the simplex range
 */
fun <T:LinearType<T>> SimplexRange4D<T>.uniform(random: Random): T {
    return value(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble())
}