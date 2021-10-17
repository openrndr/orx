package org.openrndr.extra.noise

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.sqrt
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
 * @param width the width of the area
 * @param height the height of the area
 * @param r the minimum distance between each point
 * @param tries number of candidates per point
 * @param randomOnRing generate random points on a ring with an annulus from r to 2r
 * @param random a random number generator, default value is [Random.Default]
 * @param boundsMapper a custom function to check if a point is within bounds

 * @return a list of points
 */
fun poissonDiskSampling(
        width: Double,
        height: Double,
        r: Double,
        tries: Int = 30,
        randomOnRing: Boolean = false,
        random: Random = Random.Default,
        initialPoint: Vector2 = Vector2(width/2.0, height/2.0),
        boundsMapper: ((w: Double, h: Double, v: Vector2) -> Boolean)? = null,
): List<Vector2> {
    val disk = mutableListOf<Vector2>()
    val queue = mutableListOf<Int>()

    val r2 = r * r
    val radius = r + epsilon

    val cellSize = r / sqrt(2.0)
    val rows = ceil(height / cellSize).toInt()
    val cols = ceil(width / cellSize).toInt()

    val grid = MutableList(rows * cols) { -1 }

    fun addPoint(v: Vector2) {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        val index = x + y * cols

        disk.add(v)

        grid[index] = disk.lastIndex

        queue.add(disk.lastIndex)
    }

    addPoint(initialPoint)

    val boundsRect = Rectangle(0.0, 0.0, width, height)

    while (queue.isNotEmpty()) {
        val activeIndex = queue.random(random)
        val active = disk[activeIndex]

        var candidateAccepted = false

        candidateSearch@ for (l in 0 until tries) {
            val c = if (randomOnRing) {
                active + Vector2.uniformRing(r, 2 * r, random)
            } else {
                active + Polar(random.nextDouble(0.0, 360.0), radius).cartesian
            }

            if (!boundsRect.contains(c)) continue@candidateSearch

            // check if it's within bounds
            // choose another candidate if it's not
            if (boundsMapper != null && !boundsMapper(width, height, c)) continue@candidateSearch

            val x = (c.x / cellSize).fastFloor()
            val y = (c.y / cellSize).fastFloor()

            // Check closest neighbours in a 5x5 grid
            for (ix in (-2..2)) {
                for (iy in (-2..2)) {
                    val nx = clamp(x + ix, 0, cols - 1)
                    val ny = clamp(y + iy, 0, rows - 1)

                    val neighborIdx = grid[nx + ny * cols]

                    // -1 means the grid has no sample at that point
                    if (neighborIdx == -1) continue

                    val neighbor = disk[neighborIdx]

                    // if the candidate is within one of the neighbours radius, try another candidate
                    if ((neighbor - c).squaredLength <= r2) continue@candidateSearch
                }
            }

            addPoint(c)

            candidateAccepted = true

            break
        }

        // If no candidate was accepted, remove the sample from the active list
        if (!candidateAccepted) {
            queue.remove(activeIndex)
        }
    }

    return disk
}