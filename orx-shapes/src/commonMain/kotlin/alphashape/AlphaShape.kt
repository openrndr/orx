package org.openrndr.extra.shapes.alphashape

import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Create an alpha shape from list of [Vector2]
 */
fun List<Vector2>.alphaShape(): Shape {
    return AlphaShape(this).createShape()
}

private fun circumradius(p1: Vector2, p2: Vector2, p3: Vector2): Double {
    val a = (p2 - p1).length
    val b = (p3 - p2).length
    val c = (p1 - p3).length

    return (a * b * c) / sqrt((a + b + c) * (b + c - a) * (c + a - b) * (a + b - c))
}

/**
 * Class for creating alpha shapes.
 * See the [createContour] and [createShape] methods to create an alpha shape.
 * @param points The points for which an alpha shape is calculated.
 */
class AlphaShape(val points: List<Vector2>) {
    val delaunay = Delaunay.from(points)

    private fun <A, B> Pair<A, B>.flip() = Pair(second, first)

    private fun createBase(alpha: Double): List<Pair<Int, Int>> {
        if (delaunay.points.size < 9) return emptyList()

        val triangles = delaunay.triangles
        var allEdges = mutableSetOf<Pair<Int, Int>>()
        var perimeterEdges = mutableSetOf<Pair<Int, Int>>()
        for (i in triangles.indices step 3) {
            val t0 = triangles[i] * 2
            val t1 = triangles[i + 1] * 2
            val t2 = triangles[i + 2] * 2
            val p1 = getVec(t0)
            val p2 = getVec(t1)
            val p3 = getVec(t2)
            val r = circumradius(p1, p2, p3)
            if (r < alpha) {
                val edges = listOf(Pair(t0, t1), Pair(t1, t2), Pair(t2, t0))
                for (edge in edges) {
                    val fEdge = edge.flip()
                    if (edge !in allEdges && fEdge !in allEdges) {
                        allEdges.add(edge)
                        perimeterEdges.add(edge)
                    } else {
                        perimeterEdges.remove(edge)
                        perimeterEdges.remove(fEdge)
                    }
                }
            }
        }
        return perimeterEdges.toList()
    }

    /**
     * Creates an alpha shape without holes
     * @param alpha The alpha parameter from the mathematical definition of an alpha shape.
     * If alpha is 0.0 the alpha shape consists only of the set of input points, yielding [ShapeContour.EMPTY].
     * As alpha goes to infinity, the alpha shape becomes equal to the convex hull of the input points.
     * @return A closed [ShapeContour] representing the alpha shape, or [ShapeContour.EMPTY] if the alpha shape
     * cannot be represented by a closed [ShapeContour] (e.g. because it consists of multiple disconnected components).
     */
    fun createContour(alpha: Double): ShapeContour = edgesToShapeContour(createBase(alpha))

    /**
     * Returns a closed [ShapeContour] representing an alpha shape without holes; the smallest alpha is chosen such that
     * the corresponding alpha shape contains all input points and can be represented by a closed [ShapeContour].
     */
    fun createContour(): ShapeContour = createContour(determineContourAlpha())

    /**
     * Creates an alpha shape, possibly with holes
     * @param alpha The alpha parameter from the mathematical definition of an alpha shape.
     * If alpha is 0.0 the alpha shape consists only of the set of input points, yielding [Shape.EMPTY].
     * As alpha goes to infinity, the alpha shape becomes equal to the convex hull of the input points.
     * @return A [Shape] representing the alpha shape, or [Shape.EMPTY] if the alpha shape
     * cannot be represented by a [Shape] (e.g. because it consists of multiple disconnected components).
     */
    fun createShape(alpha: Double): Shape = edgesToShape(createBase(alpha))

    /**
     * Returns a [Shape] representing an alpha shape; the smallest alpha is chosen such that the corresponding alpha
     * shape contains all input points and can be represented by a [Shape] (in particular, it consists of one component).
     */
    fun createShape(): Shape = edgesToShape(createBase(determineShapeAlpha()))

    /**
     * Creates an alpha shape
     * @param alpha The alpha parameter from the mathematical definition of an alpha shape.
     * If alpha is 0.0 the alpha shape consists only of the set of input points, yielding [ShapeContour.EMPTY].
     * As alpha goes to infinity, the alpha shape becomes equal to the convex hull of the input points.
     * @return A list of [LineSegment]s representing the perimeter of the alpha shape.
     */
    fun createSegments(alpha: Double): List<LineSegment> =
        createBase(alpha).map { LineSegment(getVec(it.first), getVec(it.second)) }

    private fun getVec(i: Int) = Vector2(delaunay.points[i], delaunay.points[i + 1])

    private fun edgesToShapeContour(edges: List<Pair<Int, Int>>): ShapeContour {
        if (edges.isEmpty()) return ShapeContour.EMPTY
        val mapping = edges.toMap()
        val segments = mutableListOf<Segment>()
        val start = edges.first().first
        var current = start
        val left = edges.map { it.first }.toMutableSet()
        for (i in edges.indices) {
            val next = mapping[current]!!
            segments.add(Segment(getVec(current), getVec(next)))
            left.remove(current)
            current = next
            if (current == start) break
        }
        return if (current == start && left.isEmpty()) {
            ShapeContour(segments, closed = true).clockwise
        } else {
            ShapeContour.EMPTY
        }
    }

    private fun edgesToShape(edges: List<Pair<Int, Int>>): Shape {
        if (edges.isEmpty()) return Shape.EMPTY
        val mapping = edges.toMap()
        val contours = mutableListOf<ShapeContour>()
        val contoursPoints = mutableListOf<List<Vector2>>()
        val left = edges.map { it.first }.toMutableSet()

        // Find closed loops and save them as contours
        while (left.isNotEmpty()) {
            val start = left.first()
            var current = start
            val segments = mutableListOf<Segment>()
            val contourPoints = mutableListOf<Vector2>()
            for (i in edges.indices) {
                val next = mapping[current]!!
                segments.add(Segment(getVec(current), getVec(next)))
                contourPoints.add(getVec(current))
                left.remove(current)
                current = next
                if (current == start) break
            }
            contourPoints.add(getVec(current))
            contoursPoints.add(contourPoints)
            if (current == start) contours.add(ShapeContour(segments, closed = true))
        }

        // Find contour that encloses all other contours, if it exists
        var enclosingContour = -1
        for (i in contours.indices) {
            var encloses = true
            for (j in contours.indices) {
                if (i == j) continue
                if (contoursPoints[j].any { it !in contours[i] }) {
                    encloses = false
                }
            }
            if (encloses) {
                enclosingContour = i
                break
            }
        }

        // If an enclosing contour exists, make a shape with it being clockwise and the other contours counterclockwise
        return if (enclosingContour < 0) {
            Shape.EMPTY
        } else {
            val orientedContours = mutableListOf<ShapeContour>()
            orientedContours.add(contours[enclosingContour].clockwise)
            for (i in contours.indices) {
                if (i != enclosingContour) orientedContours.add(contours[i].counterClockwise)
            }
            Shape(orientedContours)
        }
    }

    /**
     * Performs binary search to find the smallest alpha such that all points are inside the alpha shape.
     */
    private fun determineAlphaBase(decision: (Double) -> Boolean): Double {
        // Compute bounding box to find an upper bound for the binary search
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (i in delaunay.points.indices step 2) {
            val x = delaunay.points[i]
            val y = delaunay.points[i + 1]
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        // Perform binary search
        var lower = 0.0
        var upper = (maxX - minX).pow(2) + (maxY - minY).pow(2)
        val precision = 0.001

        while (lower < upper - precision) {
            val mid = (lower + upper) / 2
            if (decision(mid)) upper = mid else lower = mid
        }

        return upper
    }

    fun determineContourAlpha(): Double = determineAlphaBase { mid -> points.all { it in createContour(mid) } }
    fun determineShapeAlpha(): Double = determineAlphaBase { mid -> points.all { it in createShape(mid) } }
}