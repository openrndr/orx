package org.openrndr.extra.noise

import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.random.Random

fun random(
    min: Double = -1.0, max: Double = 1.0,
    random: Random = Random.Default
) =
    (random.nextDouble() * (max - min)) + min

/**
 * Generates a uniformly distributed random integer within the specified range.
 *
 * @param min The lower bound of the range (inclusive). Default is -1.
 * @param max The upper bound of the range (exclusive). Default is 2.
 * @param random An instance of Random to generate the random value. Default is Random.Default.
 * @return A random integer value within the range [min, max).
 */
fun Int.Companion.uniform(
    min: Int = -1, max: Int = 2,
    random: Random = Random.Default
) =
    (random.nextDouble() * (max - min)).toInt() + min

/**
 * Generates a random double value within the specified range [min, max].
 *
 * @param min The minimum value of the range (inclusive). Defaults to -1.0.
 * @param max The maximum value of the range (exclusive). Defaults to 1.0.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A randomly generated double value between [min, max].
 */
fun Double.Companion.uniform(
    min: Double = -1.0, max: Double = 1.0,
    random: Random = Random.Default
) =
    (random.nextDouble() * (max - min)) + min


/**
 * Generates a list of random double values within the specified range.
 *
 * @param count The number of random double values to generate.
 * @param min The minimum value (inclusive) of the range. Default is -1.0.
 * @param max The maximum value (exclusive) of the range. Default is 1.0.
 * @param random The random number generator to use. Default is Random.Default.
 * @return A list of random double values within the specified range.
 */
fun Double.Companion.uniforms(
    count: Int,
    min: Double = -1.0, max: Double = 1.0,
    random: Random = Random.Default
) =
    List(count) {
        (random.nextDouble() * (max - min)) + min
    }

fun Double.Companion.hash(
    seed: Int, x: Int,
    min: Double = -1.0, max: Double = 1.0
) = fhash1D(seed, x) * (max - min) + min


/**
 * Generates a random 2D vector with each component uniformly distributed within the specified ranges.
 *
 * @param min The minimum values for the x and y components of the vector (inclusive). Defaults to Vector2(-1, -1).
 * @param max The maximum values for the x and y components of the vector (exclusive). Defaults to Vector2(1, 1).
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A randomly generated Vector2 object with components within [min.x, max.x) and [min.y, max.y).
 */
fun Vector2.Companion.uniform(
    min: Vector2 = -ONE, max: Vector2 = ONE,
    random: Random = Random.Default
) =
    Vector2(
        Double.uniform(min.x, max.x, random),
        Double.uniform(min.y, max.y, random)
    )

/**
 * Generates a hash-based 2D vector based on the provided seed, input value, and range constraints.
 *
 * @param seed An integer used to initialize the hash generation process, providing variability in the result.
 * @param x The input value used as the basis for generating the hash values.
 * @param min The minimum bounds for both components of the resulting vector. Defaults to -ONE.
 * @param max The maximum bounds for both components of the resulting vector. Defaults to ONE.
 */
fun Vector2.Companion.hash(
    seed: Int, x: Int,
    min: Vector2 = -ONE, max: Vector2 = ONE
) =
    Vector2(
        Double.hash(seed, x, min.x, max.x),
        Double.hash(seed xor 0x7f7f7f7f, x, min.y, max.y)
    )

/**
 * Generates a random 2D vector with components uniformly distributed within the specified range.
 *
 * @param min The minimum value for both x and y components of the vector (inclusive). Default is -1.0.
 * @param max The maximum value for both x and y components of the vector (exclusive). Default is 1.0.
 * @param random The random number generator to use. Default is Random.Default.
 * @return A randomly generated Vector2 object with both x and y components within the range [min, max).
 */
fun Vector2.Companion.uniform(
    min: Double = -1.0, max: Double = 1.0,
    random: Random = Random.Default
) =
    Vector2.uniform(Vector2(min, min), Vector2(max, max), random)

/**
 * Generates a 2D hash-based vector using specified seed, input value, and range bounds.
 *
 * @param seed An integer seed value for initializing the hash generation.
 * @param x The numerical input value used for generating the hash.
 * @param min The minimum value bound for both vector components. Defaults to -1.0.
 * @param max The maximum value bound for both vector components. Defaults to 1.0.
 */
fun Vector2.Companion.hash(
    seed: Int, x: Int,
    min: Double = -1.0, max: Double = 1.0,
) =
    Vector2.hash(seed, x, Vector2(min, min), Vector2(max, max))


/**
 * Generates a random 2D vector uniformly distributed within the specified rectangular bounds.
 *
 * @param rect The rectangle within which the vector's components will be randomly generated.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A randomly generated Vector2 object with components within the bounds of the specified rectangle.
 */
fun Vector2.Companion.uniform(
    rect: Rectangle,
    random: Random = Random.Default
) =
    Vector2.uniform(
        rect.corner,
        rect.corner + rect.dimensions, random
    )

/**
 * Generates a random `IntVector2` with each component within the specified ranges.
 *
 * @param min The minimum values for the x and y components of the vector. Default is `IntVector2(-1, -1)`.
 * @param max The maximum exclusive values for the x and y components of the vector. Default is `IntVector2(2, 2)`.
 * @param random The random number generator used to generate the values. Default is `Random.Default`.
 */
fun IntVector2.Companion.uniform(
    min: IntVector2 = IntVector2(-1, -1),
    max: IntVector2 = IntVector2(2, 2),
    random: Random = Random.Default
) =
    IntVector2(
        Int.uniform(min.x, max.x, random),
        Int.uniform(min.y, max.y, random)
    )

/**
 * Generates a uniform random `IntVector2` within the specified range.
 *
 * @param min The minimum inclusive value for both components of the vector. Defaults to -1.
 * @param max The maximum exclusive value for both components of the vector. Defaults to 2.
 * @param random The random number generator to use. Defaults to `Random.Default`.
 * @return A randomly generated `IntVector2` within the specified bounds.
 */
fun IntVector2.Companion.uniform(
    min: Int = -1, max: Int = 2,
    random: Random = Random.Default
) =
    IntVector2.uniform(
        IntVector2(min, min),
        IntVector2(max, max), random
    )

/**
 * Generates a random 2D vector uniformly distributed within a ring defined by the inner and outer radii.
 *
 * @param innerRadius The inner radius of the ring. Must be less than or equal to outerRadius. Default is 0.0.
 * @param outerRadius The outer radius of the ring. Default is 1.0.
 * @param random The random number generator to use. Default is Random.Default.
 * @return A 2D vector uniformly distributed within the specified ring.
 * @throws IllegalArgumentException If innerRadius is greater than outerRadius.
 */
fun Vector2.Companion.uniformRing(
    innerRadius: Double = 0.0,
    outerRadius: Double = 1.0,
    random: Random = Random.Default
): Vector2 {
    val eps = 1E-6

    if (abs(innerRadius - outerRadius) < eps) {
        val angle = Double.uniform(-180.0, 180.0, random)
        return Polar(angle, innerRadius).cartesian

    } else if (innerRadius < outerRadius) {
        while (true) {
            uniform(-outerRadius, outerRadius, random).let {
                val squaredLength = it.squaredLength
                if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                    return it
                }
            }
        }
    } else {
        error("innerRadius (=$innerRadius) should be less or equal to outerRadius (=$outerRadius)")
    }
}

/**
 * Generates a list of `Vector2` instances, each initialized with random values within
 * the specified range.
 *
 * @param count The number of `Vector2` instances to generate.
 * @param min The minimum range for the random `Vector2` values. Defaults to `-ONE`.
 * @param max The maximum range for the random `Vector2` values. Defaults to `ONE`.
 * @param random The random number generator to use. Defaults to `Random.Default`.
 * @return A list of randomly generated `Vector2` instances.
 */
fun Vector2.Companion.uniforms(
    count: Int,
    min: Vector2 = -ONE,
    max: Vector2 = ONE,
    random: Random = Random.Default
): List<Vector2> =
    List(count) {
        Vector2.uniform(min, max, random)
    }

/**
 * Generates a list of uniformly distributed random points within a specified rectangular area.
 *
 * @param count The number of random points to generate.
 * @param rect The rectangular area within which the points will be generated.
 * @param random The random number generator to use for point generation. Defaults to [Random.Default].
 * @return A list of [Vector2] objects representing the generated random points.
 */
fun Vector2.Companion.uniforms(
    count: Int,
    rect: Rectangle,
    random: Random = Random.Default
): List<Vector2> =
    List(count) { Vector2.uniform(rect, random) }

/**
 * Generates an infinite sequence of random `Vector2` points uniformly distributed
 * within the specified rectangle.
 *
 * @param rect The rectangle within which the random `Vector2` points will be generated.
 * @param random The random number generator to use for generating points. Defaults to `Random.Default`.
 * @return A sequence of `Vector2` points uniformly distributed within the specified rectangle.
 */
fun Vector2.Companion.uniformSequence(
    rect: Rectangle,
    random: Random = Random.Default
): Sequence<Vector2> =
    sequence {
        while (true) {
            yield(uniform(rect, random))
        }
    }

/**
 * Generates a list of 2D vectors uniformly distributed within a ring defined by the inner and outer radii.
 *
 * @param count The number of vectors to generate.
 * @param innerRadius The inner radius of the ring. Must be less than or equal to outerRadius. Default is 0.0.
 * @param outerRadius The outer radius of the ring. Default is 1.0.
 * @param random The random number generator to use. Default is Random.Default.
 * @return A list of 2D vectors uniformly distributed within the specified ring.
 */
fun Vector2.Companion.uniformsRing(
    count: Int,
    innerRadius: Double = 0.0, outerRadius: Double = 1.0,
    random: Random = Random.Default
): List<Vector2> =
    List(count) {
        Vector2.uniformRing(innerRadius, outerRadius, random)
    }

/**
 * Generates a random vector with each component uniformly distributed between the specified minimum and maximum bounds.
 *
 * @param min The minimum bound for the vector components. Defaults to -1.0 for all components.
 * @param max The maximum bound for the vector components. Defaults to 1.0 for all components.
 * @param random The source of randomness. Defaults to the system default random generator.
 * @return A random vector with each component uniformly distributed between the specified bounds.
 */
fun Vector3.Companion.uniform(min: Double = -1.0, max: Double = 1.0, random: Random = Random.Default): Vector3 =
    Vector3.uniform(Vector3(min, min, min), Vector3(max, max, max), random)

/**
 * Generates a random vector with components uniformly distributed between the specified minimum and maximum bounds.
 *
 * @param min The minimum bound for each component of the vector. Defaults to a vector with all components set to -1.
 * @param max The maximum bound for each component of the vector. Defaults to a vector with all components set to 1.
 * @param random The source of randomness. Defaults to the system default random generator.
 * @return A random vector with components uniformly distributed between the specified bounds.
 */
fun Vector3.Companion.uniform(
    min: Vector3 = -ONE,
    max: Vector3 = ONE,
    random: Random = Random.Default
): Vector3 {
    return Vector3(
        Double.uniform(min.x, max.x, random),
        Double.uniform(min.y, max.y, random),
        Double.uniform(min.z, max.z, random)
    )
}

/**
 * Generates a random 3D vector within a uniform ring defined by the provided inner and outer radii.
 * The ring resides in a 3D space, and vectors are uniformly distributed within the specified range.
 *
 * @param innerRadius The inner radius of the ring. Default is 0.0.
 * @param outerRadius The outer radius of the ring. Default is 1.0.
 * @param random The random number generator to be used for generating the vector. Default is Random.Default.
 * @return A random 3D vector within the specified uniform ring.
 */
fun Vector3.Companion.uniformRing(
    innerRadius: Double = 0.0,
    outerRadius: Double = 1.0,
    random: Random = Random.Default
): Vector3 {
    while (true) {
        uniform(-outerRadius, outerRadius, random).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

/**
 * Generates a list of uniformly distributed random `Vector3` instances.
 *
 * @param count The number of `Vector3` instances to generate.
 * @param min The minimum value for each component of the `Vector3`. Defaults to -1.0.
 * @param max The maximum value for each component of the `Vector3`. Defaults to 1.0.
 * @param random The random number generator to use. Defaults to `Random.Default`.
 * @return A list of uniformly distributed random `Vector3` instances.
 */
fun Vector3.Companion.uniforms(
    count: Int,
    min: Double = -1.0,
    max: Double = 1.0,
    random: Random = Random.Default
): List<Vector3> =
    List(count) {
        Vector3.uniform(min, max, random)
    }


/**
 * Generates a list of uniformly distributed random `Vector3` instances.
 *
 * @param count The number of `Vector3` instances to generate.
 * @param min The minimum bound for each `Vector3` component. Defaults to `-ONE`.
 * @param max The maximum bound for each `Vector3` component. Defaults to `ONE`.
 * @param random The random number generator used for generating components. Defaults to `Random.Default`.
 * @return A list of uniformly distributed random `Vector3` instances.
 */
fun Vector3.Companion.uniforms(
    count: Int,
    min: Vector3 = -ONE,
    max: Vector3 = ONE,
    random: Random = Random.Default
): List<Vector3> =
    List(count) {
        Vector3.uniform(min, max, random)
    }

/**
 * Generates a list of uniformly distributed random vectors within a ring defined by inner and outer radii.
 *
 * @param count The number of vectors to generate.
 * @param innerRadius The inner radius of the ring. Defaults to 0.0.
 * @param outerRadius The outer radius of the ring. Defaults to 1.0.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A list of randomly generated vectors uniformly distributed within the ring.
 */
fun Vector3.Companion.uniformsRing(
    count: Int,
    innerRadius: Double = 0.0, outerRadius: Double = 1.0,
    random: Random = Random.Default
): List<Vector3> =
    List(count) {
        Vector3.uniformRing(innerRadius, outerRadius, random)
    }


/**
 * Generates a random 4-dimensional vector with each component sampled uniformly
 * from a specified range between [min] and [max].
 *
 * @param min The minimum value of the range for all components. Default is -1.0.
 * @param max The maximum value of the range for all components. Default is 1.0.
 * @param random The random number generator to use. Default is `Random.Default`.
 * @return A `Vector4` instance with random components within the specified range.
 */
fun Vector4.Companion.uniform(min: Double = -1.0, max: Double = 1.0, random: Random = Random.Default): Vector4 =
    Vector4.uniform(Vector4(min, min, min, min), Vector4(max, max, max, max), random)

/**
 * Generates a random 4-dimensional vector where each component is uniformly distributed within the provided range.
 *
 * @param min The minimum values for each component of the vector. Defaults to -ONE.
 * @param max The maximum values for each component of the vector. Defaults to ONE.
 * @param random The random number generator to use. Defaults to Random.Default.
 * @return A 4-dimensional vector with components uniformly distributed between the specified minimum and maximum values.
 */
fun Vector4.Companion.uniform(
    min: Vector4 = -ONE,
    max: Vector4 = ONE,
    random: Random = Random.Default
): Vector4 {
    return Vector4(
        Double.uniform(min.x, max.x, random),
        Double.uniform(min.y, max.y, random),
        Double.uniform(min.z, max.z, random),
        Double.uniform(min.w, max.w, random)
    )
}

/**
 * Generates a uniformly distributed random 4D vector within a ring-shaped area defined by an inner and outer radius.
 *
 * @param innerRadius The minimum radius of the ring. Defaults to 0.0.
 * @param outerRadius The maximum radius of the ring. Defaults to 1.0.
 * @param random An instance of Random used to generate the random vector. Defaults to Random.Default.
 * @return A random 4D vector within the specified ring.
 */
fun Vector4.Companion.uniformRing(
    innerRadius: Double = 0.0,
    outerRadius: Double = 1.0,
    random: Random = Random.Default
): Vector4 {
    while (true) {
        uniform(-outerRadius, outerRadius, random).let {
            val squaredLength = it.squaredLength
            if (squaredLength >= innerRadius * innerRadius && squaredLength < outerRadius * outerRadius) {
                return it
            }
        }
    }
}

