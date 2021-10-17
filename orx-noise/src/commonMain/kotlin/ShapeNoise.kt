package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.random.Random

fun ShapeProvider.uniform(random: Random = Random.Default): Vector2 {
    val shape = shape
    return Vector2.uniformSequence(shape.bounds, random).first {
        shape.contains(it)
    }
}

fun ShapeProvider.poissonDiskSampling(
    r: Double,
    tries: Int = 30,
    random: Random = Random.Default
): List<Vector2> {
    val shape = shape
    val bounds = shape.bounds
    val poissonBounds = Rectangle(0.0, 0.0, bounds.width, bounds.height)

    val initialPoint = this.uniform(random).map(bounds, poissonBounds)

    return poissonDiskSampling(bounds.width, bounds.height, r, tries, false, random, initialPoint) { _, _, point ->
        val contourPoint = point.map(poissonBounds, bounds)
        shape.contains(contourPoint)
    }.map {
        it.map(poissonBounds, bounds)
    }
}