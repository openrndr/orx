package org.openrndr.extra.noise.simplexrange

import org.openrndr.extra.math.simplexrange.simplexUpscale
import org.openrndr.math.LinearType
import kotlin.random.Random

/**
 * Generates a random value within the convex hull of the elements in the list using a uniform
 * distribution over the simplex formed by the elements, optionally applying bias to the distribution.
 *
 * @param random The random number generator used to produce random values. Defaults to `Random.Default`.
 * @param biasOrder The number of iterations to apply bias adjustments to the weights. Defaults to 0.
 * @param biasAmount The magnitude of the bias adjustment applied during each iteration. Defaults to 0.0.
 * @return A value of type `T` representing the weighted interpolation of the list elements,
 *         with weights sampled uniformly or with bias adjustments if specified.
 */
fun <T : LinearType<T>> List<T>.uniformSimplex(random: Random = Random.Default,
                                               biasOrder: Int = 0,
                                               biasAmount: Double = 0.0): T {
    return when (size) {
        0 -> error("")
        1 -> this[0]
        2 -> {
            val x = random.nextDouble()
            this[0] * (1.0 - x) + this[1] * x
        }

        else -> {
            val r = DoubleArray(size - 1) { random.nextDouble() }
            val b = simplexUpscale(r)

            if (biasOrder > 0) {
                for (i in 0 until biasOrder) {
                    b[random.nextInt(b.size)] += random.nextDouble(biasAmount)
                }
                val sum = b.sum()
                for (i in 0 until b.size) {
                    b[i] = b[i] / sum
                }
            }
            var result = this[0] * b[0]
            for (i in 1 until size) {
                result += this[i] * b[i]
            }
            result
        }
    }
}