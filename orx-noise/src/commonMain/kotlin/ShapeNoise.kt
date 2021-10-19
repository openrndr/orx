package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.random.Random

fun ShapeProvider.uniform(distanceToEdge: Double = 0.0, random: Random = Random.Default): Vector2 {
    val shape = shape
    require(!shape.empty)
    var attempts = 0
    return Vector2.uniformSequence(shape.bounds, random).first {
        attempts++
        require(attempts < 100)
        if (distanceToEdge == 0.0) {
            shape.contains(it)
        } else {
            shape.contains(it) && shape.contours.minOf { c -> c.nearest(it).position.distanceTo(it) } > distanceToEdge
        }
    }
}

fun ShapeProvider.scatter(
    pointDistance: Double,
    distanceToEdge: Double = 0.0,
    tries: Int = 30,
    random: Random = Random.Default
): List<Vector2> {
    val shape = shape
    if (shape.empty) {
        return emptyList()
    }
    val bounds = shape.bounds
    val poissonBounds = Rectangle(0.0, 0.0, bounds.width, bounds.height)

    val initialPoints = shape.splitCompounds().flatMap { compound ->
        compound.outline.segments.map {
            val t = random.nextDouble()
            (it.position(t) - it.normal(t).normalized * distanceToEdge)
        }.filter { compound.contains(it) && compound.outline.nearest(it).position.distanceTo(it) >= distanceToEdge-1E-1 }.map {
            it.map(bounds, poissonBounds)
        }
    }

    val candidatePoints = mutableListOf<Vector2>()
    for (point in initialPoints) {
        if ((candidatePoints.map { it.distanceTo(point) }.minOrNull() ?: Double.POSITIVE_INFINITY) >= pointDistance) {
            candidatePoints.add(point)
        }
    }


    if (candidatePoints.isEmpty()) {
        return emptyList()
    }

    return poissonDiskSampling(
        bounds.width,
        bounds.height,
        pointDistance,
        tries,
        false,
        random,
        candidatePoints,
    ) { _, _, point ->
        val contourPoint = point.map(poissonBounds, bounds)
        if (distanceToEdge == 0.0) {
            shape.contains(contourPoint)
        } else {
            shape.contains(contourPoint) && shape.contours.minOf { c ->
                c.nearest(contourPoint).position.distanceTo(contourPoint)
            } > distanceToEdge
        }
    }.map {
        it.map(poissonBounds, bounds)
    }
}