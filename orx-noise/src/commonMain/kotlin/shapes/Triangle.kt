package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Triangle
import kotlin.random.Random


/**
 * Generates a list of 2D points distributed uniformly over a collection of triangles.
 *
 * @param count The number of random points to generate.
 * @param random An optional random number generator to use. Defaults to `Random.Default`.
 * @return A list of uniformly distributed 2D points over the triangles.
 */
fun List<Triangle>.uniform(count: Int, random: Random = Random.Default): List<Vector2> {
    val totalArea = this.sumOf { it.area }
    val randoms = (0 until count).map {
        Double.uniform(0.0, totalArea, random = random)
    }.sorted()
    val result = mutableListOf<Vector2>()
    var idx = 0
    var sum = 0.0
    for (t in this) {
        sum += t.area
        while (idx <= randoms.lastIndex && sum > randoms[idx]) {
            result.add(t.uniform(random))
            idx++
        }
    }
    return result
}

/**
 * Generates a list of random points, distributed across a list of triangles,
 * weighted by their respective areas. The points are generated using a hash-based
 * randomization approach.
 *
 * @param count The number of random points to generate.
 * @param seed The seed for the hash function, used to produce deterministic randomization.
 * @param x An optional offset value for the hash, defaulting to 0.
 * @return A list of `Vector2` points that are distributed among the triangles according to their areas.
 */
fun List<Triangle>.hash(count: Int, seed: Int = 0, x: Int = 0): List<Vector2> {
    val totalArea = this.sumOf { it.area }
    val randoms = (0 until count).map {
        Pair(x + it, fhash1D(seed, x + it) * totalArea)
    }.sortedBy { it.second }
    val result = mutableListOf<Vector2>()
    var idx = 0
    var sum = 0.0
    for (t in this) {
        sum += t.area
        while (idx <= randoms.lastIndex && sum > randoms[idx].second) {
            result.add(t.hash(seed, randoms[idx].first))
            idx++
        }
    }
    return result
}


/**
 * Generates a uniformly distributed random point within the `Triangle`.
 *
 * @param random An optional random number generator to use. Defaults to `Random.Default`.
 * @return A `Vector2` representing a random point within the `Triangle`.
 */
fun Triangle.uniform(random: Random = Random.Default): Vector2 {
    return position(random.nextDouble(), random.nextDouble())
}


/**
 * Generates a random point within the bounds of the `Triangle` using a hash-based approach.
 *
 * @param seed An integer seed for the hash function, used to produce deterministic random results for the same seed.
 * @param x An integer input to the hash function, adding further variation to the generated point.
 * @return A `Vector2` representing a random point within the `Triangle`, based on the provided `seed` and `x`.
 */
fun Triangle.hash(seed: Int, x: Int): Vector2 {
    val u = fhash1D(seed, x)
    val v = fhash1D(seed, u.toRawBits().toInt() + x)
    return position(u, v)
}
