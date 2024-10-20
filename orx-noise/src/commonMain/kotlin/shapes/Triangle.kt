package org.openrndr.extra.noise.shapes

import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Triangle
import kotlin.random.Random

/**
 * Generate [count] uniform samples from a list of [Triangle]s
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

/** Generates a random point that lies inside the [Triangle]. */
fun Triangle.uniform(random: Random = Random.Default): Vector2 {
    return position(random.nextDouble(), random.nextDouble())
}


fun Triangle.hash(seed: Int, x: Int): Vector2 {
    val u = fhash1D(seed, x)
    val v = fhash1D(seed, u.toRawBits().toInt() + x)
    return position(u, v)
}
