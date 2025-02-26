package org.openrndr.extra.noise.simplexrange

import org.openrndr.math.LinearType
import org.openrndr.extra.math.simplexrange.SimplexRange4D
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import org.openrndr.extra.math.simplexrange.SimplexRange2D
import kotlin.math.pow

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
 * Generates a uniformly distributed value within the SimplexRange2D.
 *
 * @param random the random number generator used to produce random values.
 * @return a value of type T sampled uniformly within the 2D simplex range.
 */
fun <T:LinearType<T>> SimplexRange2D<T>.uniformPower(exp: Double, random: Random = Random.Default): T {
    val b = upscale(random.nextDouble(), random.nextDouble())
    for (i in 0 until b.size) {
        b[i] = b[i].pow(exp)
    }
    val sum = b.sum()
    return x0 * b[0] / sum + x1 * b[1] / sum + x2 * b[2] / sum
}


/**
 * Generates a random point within the simplex represented by the `SimplexRange2D`,
 * forming an interpolation of the three control points `x0`, `x1`, `x2` using a weighted
 * random combination normalized to sum to 1.
 *
 * @param random The random number generator used to produce the random weights.
 * @return A randomly interpolated point of type `T` within the simplex.
 */
fun <T:LinearType<T>> SimplexRange2D<T>.uniformCube(random: Random): T {
    val r = DoubleArray(3) { random.nextDouble() }
    val sum = r.sum()
    if (sum > 0.0) {
        for (i in 0 until r.size) {
            r[i] /= sum
        }
    }
    return this.x0 * r[0] + x1 * r[1] + x2 * r[2]
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
 * Generates a random point within the simplex represented by the `SimplexRange3D`,
 * forming an interpolation of the four control points using a weighted random combination normalized to sum to 1.
 *
 * @param random The random number generator used to produce the random weights.
 * @return A randomly interpolated point of type `T` within the simplex.
 */
fun <T:LinearType<T>> SimplexRange3D<T>.uniformCube(random: Random): T {
    val r = DoubleArray(4) { random.nextDouble() }
    val sum = r.sum()
    if (sum > 0.0) {
        for (i in 0 until r.size) {
            r[i] /= sum
        }
    }
    return this.x0 * r[0] + x1 * r[1] + x2 * r[2] + x3 * r[3]
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

/**
 * Generates a random point within the simplex represented by the `SimplexRange4D`,
 * forming an interpolation of the five control points using a weighted random combination normalized to sum to 1.
 *
 * @param random The random number generator used to produce the random weights.
 * @return A randomly interpolated point of type `T` within the simplex.
 */
fun <T:LinearType<T>> SimplexRange4D<T>.uniformCube(random: Random): T {
    val r = DoubleArray(5) { random.nextDouble() }
    val sum = r.sum()
    if (sum > 0.0) {
        for (i in 0 until r.size) {
            r[i] /= sum
        }
    }
    return this.x0 * r[0] + x1 * r[1] + x2 * r[2] + x3 * r[3] + x4 * r[4]
}
