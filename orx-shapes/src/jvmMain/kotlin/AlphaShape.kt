package org.openrndr.extra.shapes

import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private fun circumradius(p1: Vector2, p2: Vector2, p3: Vector2): Double {
    val a = (p2 - p1).length
    val b = (p3 - p2).length
    val c = (p1 - p3).length

    return (a*b*c) / sqrt((a+b+c)*(b+c-a)*(c+a-b)*(a+b-c))
}

/**
 * Class for creating alpha shapes.
 * Use the [create] method to create an alpha shape.
 * @param points The points for which an alpha shape is calculated.
 */
class AlphaShape(val points: List<Vector2>) {
    val delaunay = Delaunay.from(points)

    private fun <A, B> Pair<A, B>.flip() = Pair(second, first)

    /**
     * Creates an alpha shape.
     * @param alpha The alpha parameter from the mathematical definition of an alpha shape.
     * If alpha is 0.0 the alpha shape consists only of the set of input points, yielding [ShapeContour.EMPTY].
     * As alpha goes to infinity, the alpha shape becomes equal to the convex hull of the input points.
     * @return A closed [ShapeContour] representing the outer boundary of the alpha shape.
     */
    fun create(alpha: Double): ShapeContour {
        if (delaunay.points.size < 9) return ShapeContour.EMPTY

        val triangles = delaunay.triangles
        var allEdges = mutableSetOf<Pair<Int, Int>>()
        var perimeterEdges = mutableSetOf<Pair<Int, Int>>()
        for (i in triangles.indices step 3){
            val t0 = triangles[i] * 2
            val t1 = triangles[i + 1] * 2
            val t2 = triangles[i + 2] * 2
            val p1 = getVec(t0)
            val p2 = getVec(t1)
            val p3 = getVec(t2)
            val r = circumradius(p1, p2, p3)
            if (r < alpha){
                val edges = listOf(Pair(t0, t1), Pair(t1, t2), Pair(t2, t0))
                for (edge in edges){
                    val fEdge = edge.flip()
                    if (edge !in allEdges && fEdge !in allEdges){
                        allEdges.add(edge)
                        perimeterEdges.add(edge)
                    } else {
                        perimeterEdges.remove(edge)
                        perimeterEdges.remove(fEdge)
                    }
                }
            }
        }
        return edgesToShapeContour(perimeterEdges.toList())
    }

    /**
     * Returns the alpha shape with the smallest alpha such that all input points are contained in the alpha shape.
     */
    fun create(): ShapeContour = create(determineAlpha())

    private fun getVec(i: Int) = Vector2(delaunay.points[i], delaunay.points[i + 1])

    private fun edgesToShapeContour(edges: List<Pair<Int, Int>>): ShapeContour {
        if (edges.isEmpty()) return ShapeContour.EMPTY
        val mapping = edges.toMap()
        val segments = mutableListOf<Segment>()
        val start = edges.first().first
        var current = start
        repeat(edges.size) {
            val next = mapping[current]!!
            segments.add(Segment(getVec(current), getVec(next)))
            current = next
        }
        return if (current == start) {
            ShapeContour(segments, closed = true)
        } else {
            ShapeContour.EMPTY
        }
    }

    /**
     * Performs binary search to find the smallest alpha such that all points are inside the alpha shape.
     */
    fun determineAlpha(): Double {
        // Compute bounding box to find an upper bound for the binary search
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (i in delaunay.points.indices step 2){
            val x = delaunay.points[i]
            val y = delaunay.points[i+1]
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        // Perform binary search
        var lower = 0.0
        var upper = (maxX - minX).pow(2) + (maxY - minY).pow(2)
        val precision = 0.001

        while(lower < upper - precision){
            val mid = (lower + upper)/2
            val polygon = create(mid)
            if (points.all { it in polygon }) upper = mid else lower = mid
        }

        return upper
    }
}