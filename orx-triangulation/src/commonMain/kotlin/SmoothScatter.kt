package org.openrndr.extra.triangulation

import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeProvider
import org.openrndr.shape.bounds
import kotlin.random.Random

fun ShapeProvider.smoothScatterSeq(
    placementRadius: Double,
    distanceToEdge: Double = placementRadius * 2.0,
    smoothing: Double = 0.5,
    random: Random = Random.Default
) = sequence {
    val boundaryPointSets = this@smoothScatterSeq.shape.contours.map {
        it.equidistantPositions((it.length / placementRadius).toInt())
    }

    val boundaryPoints = boundaryPointSets.flatten()
    val interiorPoints = this@smoothScatterSeq.shape.scatter(
        placementRadius = placementRadius, distanceToEdge = distanceToEdge, random = random
    )

    val bounds = interiorPoints.bounds.offsetEdges(100.0)
    var relaxedPoints = interiorPoints

    while (true) {
        val dt = (relaxedPoints + boundaryPoints)
        val v = dt.voronoiDiagram(bounds)

        relaxedPoints = relaxedPoints.mapIndexed { index, it ->
            val c = v.cellCentroid(index)
            if (c.x == c.x && c.y == c.y) {
                it * smoothing + c * (1.0 - smoothing)
            } else {
                it
            }
        }
        yield(relaxedPoints)
    }
}

fun ShapeProvider.smoothScatterWeightedSeq(
    placementRadius: Double,
    distanceToEdge: Double = placementRadius * 2.0,
    smoothing: Double = 0.5,
    random: Random = Random.Default
) = sequence {
    val boundaryPointSets = this@smoothScatterWeightedSeq.shape.contours.map {
        it.equidistantPositions((it.length / placementRadius).toInt())
    }

    val boundaryPoints = boundaryPointSets.flatten()
    val interiorPoints = this@smoothScatterWeightedSeq.shape.scatter(
        placementRadius = placementRadius, distanceToEdge = distanceToEdge, random = random
    )

    val bounds = interiorPoints.bounds.offsetEdges(100.0)
    var relaxedPoints = interiorPoints

    fun isBoundaryPoint(i: Int) = i >= interiorPoints.size

    val targetAreas = interiorPoints.map { if (random.nextDouble() < 0.1) 450.0 else null }

    while (true) {
        val dt = (relaxedPoints + boundaryPoints)
        val v = dt.voronoiDiagram(bounds)


        relaxedPoints = relaxedPoints.mapIndexed { index, it ->
            val c = v.cellCentroid(index)
            if (c.x == c.x && c.y == c.y) {
                it * smoothing + c * (1.0 - smoothing)
            } else {
                it
            }
        }
        val resolvedPoints = relaxedPoints.map { it }.toMutableList()


        for (i in interiorPoints.indices) {

            if (targetAreas[i] != null) {

                val targetArea = targetAreas[i]!!
                val cellArea = v.cellArea(i)
                val cellCentroid = v.cellCentroid(i)
                val areaDiff = targetArea - cellArea

                val ns = v.neighbors(i).filter { !isBoundaryPoint(it) }.toList()

                var force: Vector2
                val scale = 1.0 / ns.size
                for (n in ns) {
                    force = v.cellCentroid(n) - cellCentroid
                    resolvedPoints[n] += force.normalized * (areaDiff * 0.01) * scale
                }
            }
            relaxedPoints = resolvedPoints
        }
        yield(relaxedPoints)
    }
}


fun ShapeProvider.smoothScatter(
    placementRadius: Double,
    distanceToEdge: Double = placementRadius * 2.0,
    iterations: Int = 10,
    smoothing: Double = 0.5,
    random: Random = Random.Default
): List<Vector2> {

    val seq = smoothScatterSeq(placementRadius, distanceToEdge, smoothing, random).iterator()

    for (i in 0 until iterations - 1) {
        seq.next()
    }
    return seq.next()
}