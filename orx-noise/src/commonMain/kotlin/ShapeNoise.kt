package org.openrndr.extra.noise

import org.openrndr.extra.hashgrid.HashGrid
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

fun ShapeProvider.multiScatter(
    radii: List<Pair<Double, Double>>,
    distanceToEdge: Double = 0.0,
    tries: Int = 30,
    random: Random = Random.Default
) : List<Pair<Double, List<Vector2>>> {

    val obstacles = mutableListOf<Pair<Double, List<Vector2>>>()
    val result = mutableListOf<Pair<Double, List<Vector2>>>()
    for ((placementRadius, objectRadius) in radii) {
        val points = scatter(placementRadius, objectRadius, distanceToEdge, tries, obstacles, random)
        obstacles.add(Pair(objectRadius, points))
        result.add(Pair(objectRadius, points))
    }
    return result
}


fun ShapeProvider.scatter(
    placementRadius: Double,
    objectRadius: Double = placementRadius,
    distanceToEdge: Double = 0.0,
    tries: Int = 30,
    obstacles: List<Pair<Double, List<Vector2>>> = emptyList(),
    random: Random = Random.Default
): List<Vector2> {
    val shape = shape
    if (shape.empty) {
        return emptyList()
    }
    val bounds = shape.bounds

    val obstacleHashGrids = obstacles.map { (obstacleRadius, points) ->
        val hg = HashGrid(obstacleRadius + objectRadius)
        for (point in points) {
            hg.insert(point)
        }
        hg
    }

    fun Segment.randomPoints(count: Int) = sequence {
        for (i in 0 until count) {
            val t = random.nextDouble()
            yield(position(t) - normal(t).normalized * distanceToEdge)
        }
    }


    val initialPointHashGrid = HashGrid(placementRadius)
    val initialPoints = shape.splitCompounds().flatMap { compound ->
        compound.outline.segments.mapNotNull {
            val point = it.randomPoints(20).firstOrNull { v ->
                obstacleHashGrids.all { ohg -> ohg.isFree(v) } &&
                        initialPointHashGrid.isFree(v) &&
                        compound.contains(v) &&
                        compound.outline.nearest(v).position.distanceTo(v) >= distanceToEdge - 1E-1
            }
            if (point != null) {
                initialPointHashGrid.insert(point)
            }
            point
        }
    }

    require(initialPoints.isNotEmpty() || obstacles.isNotEmpty())

    val candidatePoints = mutableListOf<Vector2>()
    for (point in initialPoints) {
        if ((candidatePoints.map { it.distanceTo(point) }.minOrNull() ?: Double.POSITIVE_INFINITY) >= placementRadius * 2.0) {
            candidatePoints.add(point)
        }
    }

    if (candidatePoints.isEmpty()) {
        return emptyList()
    }

    return poissonDiskSampling(
        bounds,
        placementRadius * 2.0,
        tries,
        true,
        random,
        candidatePoints,
        obstacleHashGrids = obstacleHashGrids,
    ) { point ->
        if (distanceToEdge == 0.0) {
            shape.contains(point)
        } else {
            shape.contains(point) && shape.contours.minOf { c ->
                c.nearest(point).position.distanceTo(point)
            } > distanceToEdge
        }
    }
}