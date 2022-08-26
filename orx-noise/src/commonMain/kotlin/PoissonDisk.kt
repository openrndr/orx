package org.openrndr.extra.noise

import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.random.Random

/*
* TODO v2
*  * Generalize to 3 dimensions
*/

internal const val epsilon = 0.0000001

/**
 * Creates a random point distribution on a given area
 * Each point gets n [tries] at generating the next point
 * By default the points are generated along the circumference of r + epsilon to the point
 * They can also be generated on a ring like in the original algorithm from Robert Bridson
 *
 * @param bounds the rectangular bounds of the area to generate points in
 * @param radius the minimum distance between each point
 * @param tries number of candidates per point
 * @param randomOnRing generate random points on a ring with an annulus from r to 2r
 * @param random a random number generator, default value is [Random.Default]
 * @param initialPoints a list of points in sampler space, these points will not be tested against [r]
 * @param obstacleHashGrids a list of obstacles to avoid, defined by points and radii
 * @param boundsMapper a custom function to check if a point is within bounds

 * @return a list of points
 */
fun poissonDiskSampling(
    bounds: Rectangle,
    radius: Double,
    tries: Int = 30,
    randomOnRing: Boolean = true,
    random: Random = Random.Default,
    initialPoints: List<Vector2> = listOf(bounds.center),
    obstacleHashGrids: List<HashGrid> = emptyList(),
    boundsMapper: ((v: Vector2) -> Boolean)? = null,
): List<Vector2> {
    val disk = mutableListOf<Vector2>()
    val queue = mutableSetOf<Pair<Vector2, Double>>()
    val hashGrid = HashGrid(radius)

    fun addPoint(v: Vector2, radius: Double) {
        hashGrid.insert(v)
        disk.add(v)
        queue.add(Pair(v, radius))
    }

    for (initialPoint in initialPoints) {
        addPoint(initialPoint, radius)
    }

    for (ohg in obstacleHashGrids) {
        for (point in ohg.points()) {
            queue.add(Pair(point.first, ohg.radius))
        }
    }

    while (queue.isNotEmpty()) {
        val queueItem = queue.random(random)
        val (active, activeRadius) = queueItem
        var candidateAccepted = false
        candidateSearch@ for (l in 0 until tries) {
            val c = if (randomOnRing) {
                active + Vector2.uniformRing(activeRadius, 2 * activeRadius- epsilon, random)
            } else {
                active + Polar(random.nextDouble(0.0, 360.0), activeRadius).cartesian
            }
            if (!bounds.contains(c)) continue@candidateSearch

            if (!hashGrid.isFree(c) || obstacleHashGrids.any { !it.isFree(c) })
                continue@candidateSearch

            // check if the candidate point is within bounds
            // EJ: This is somewhat counter-intuitively moved to the last stage in the process;
            //     It turns out that the above neighbour search is much more affordable than the bounds check in the
            //     case of complex bounds (such as described by Shapes or ShapeContours). A simple benchmark shows a
            //     speed-up of roughly 300%
            if (boundsMapper != null && !boundsMapper(c)) continue@candidateSearch

            addPoint(c, radius)
            candidateAccepted = true
            break
        }

        // If no candidate was accepted, remove the sample from the active list
        if (!candidateAccepted) {
            queue.remove(queueItem)
        }
    }
    return disk
}

