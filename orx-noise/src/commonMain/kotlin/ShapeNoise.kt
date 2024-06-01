package org.openrndr.extra.noise

import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.random.Random

/**
 * Returns a random [Vector2] point located inside a [ShapeProvider] while
 * maintaining a distance to the edge of the shape of [distanceToEdge] units.
 */
fun ShapeProvider.uniform(distanceToEdge: Double = 0.0, random: Random = Random.Default): Vector2 {
    val shape = shape
    require(!shape.empty)
    var attempts = 0
    val innerBounds = shape.bounds.offsetEdges(-distanceToEdge.coerceAtLeast(0.0))
    return Vector2.uniformSequence(innerBounds, random).first {
        attempts++
        require(attempts < 100)
        if (distanceToEdge == 0.0) {
            shape.contains(it)
        } else {
            shape.contains(it) && shape.contours.minOf { c -> c.nearest(it).position.distanceTo(it) } > distanceToEdge
        }
    }
}

/**
 * Generate [sampleCount] uniformly distributed points inside the area of [ShapeProvider]
 */
fun ShapeProvider.uniform(sampleCount: Int, random: Random = Random.Default) : List<Vector2> = shape.triangulation.uniform(sampleCount, random)


/**
 * Returns a list of pairs in which the first component is a radius and the
 * second component a list of [Vector2] positions of items with that radius.
 *
 * [multiScatter] is a variation of [scatter] not limited to items of equal radius.
 *
 * The [radii] argument contains a list of pairs with `placementRadius` and `objectRadius`.
 *
 * The algorithm iterates a maximum of [tries] times trying to find 2D points
 * that maintain the separations to each other specified via [radii] while
 * keeping a [distanceToEdge] distance to the contour of the shape.
 */
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

/**
 * Returns a list of 2D points contained in the [ShapeProvider]. The algorithm
 * iterates a maximum of [tries] times trying to find points that maintain
 * the separation to each other specified via [placementRadius] while
 * keeping a [distanceToEdge] distance to the contour of the shape.
 *
 * It is possible to include [obstacles] to avoid. The optional
 * list of obstacles contains pairs, each pair has a radius and a list of
 * 2D locations. [objectRadius] defines a margin to keep around the obstacles.
 */
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

    fun Segment2D.randomPoints(count: Int) = sequence {
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
