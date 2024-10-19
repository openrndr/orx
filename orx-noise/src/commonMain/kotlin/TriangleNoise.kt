package org.openrndr.extra.noise

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
            result.add(t.randomPoint(random))
            idx++
        }
    }
    return result
}