package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.Triangle
import org.openrndr.shape.contains

/**
 * Computes a constrained Delaunay triangulation (CDT) of a [Shape].
 *
 * Every edge of every (closed) contour of [shape] is guaranteed to appear as an
 * edge in the resulting triangulation, and triangles that fall outside the shape --
 * including triangles inside holes -- are discarded. Contours are combined using
 * [Shape]'s own containment rule ([org.openrndr.shape.contains]), so holes, islands
 * inside holes, and compound shapes are all handled.
 *
 * Internally this is built on [Tripack], a direct port of the core algorithms of
 * Robert Renka's TRIPACK (every contour edge is forced into an unconstrained Delaunay
 * triangulation via repeated diagonal swapping), followed by discarding triangles
 * whose centroid falls outside [shape].
 *
 * @param shape a [Shape] whose contours are closed and consist solely of linear
 * segments (see [Shape.linear]).
 * @param interiorPoints additional, unconstrained points to include in the
 * triangulation, for example to refine/densify the mesh. Every point must lie
 * inside [shape].
 */
class ConstrainedDelaunayTriangulation(
    val shape: Shape,
    interiorPoints: List<Vector2> = emptyList()
) : AbstractDelaunayTriangulation {

    /**
     * The combined vertex positions used for triangulation: [interiorPoints] followed
     * by the vertices of all contours of [shape], in that order.
     */
    override val points: List<Vector2>

    private val indices: List<IntArray>

    init {
        require(shape.linear) {
            "ConstrainedDelaunayTriangulation requires a shape with only linear segments"
        }

        val contours = shape.contours.filter { it.closed && it.segments.size >= 3 }
        require(contours.isNotEmpty()) {
            "ConstrainedDelaunayTriangulation requires at least one closed contour with 3 or more points"
        }

        require(interiorPoints.all { it in shape }) {
            "ConstrainedDelaunayTriangulation: all interior points must lie inside the shape"
        }

        val allPoints = mutableListOf<Vector2>()
        allPoints.addAll(interiorPoints)

        val contourRanges = contours.map { contour ->
            val start = allPoints.size
            allPoints.addAll(contour.segments.map { it.start })
            start until allPoints.size
        }

        points = allPoints

        val mesh = Tripack(points.size)
        for ((i, p) in points.withIndex()) {
            mesh.setPoint(i + 1, p.x, p.y)
        }
        mesh.build(points.size)

        for (range in contourRanges) {
            val count = range.last - range.first + 1
            for (i in 0 until count) {
                val a = range.first + i + 1
                val b = range.first + (i + 1) % count + 1
                mesh.forceEdge(a, b)
            }
        }

        indices = mesh.enumerateTriangles()
            .map { intArrayOf(it[0] - 1, it[1] - 1, it[2] - 1) }
            .filter { (a, b, c) ->
                val centroid = (points[a] + points[b] + points[c]) / 3.0
                centroid in shape
            }
    }

    /** Adjacency derived from [indices] (i.e. from the surviving, inside-the-shape triangles only). */
    private val neighborSets: List<Set<Int>> by lazy {
        val sets = Array(points.size) { mutableSetOf<Int>() }
        for ((a, b, c) in indices) {
            sets[a].add(b); sets[a].add(c)
            sets[b].add(a); sets[b].add(c)
            sets[c].add(a); sets[c].add(b)
        }
        sets.asList()
    }

    /** The resulting triangles as index triples into [points], in counter-clockwise order. */
    override fun triangleIndices(): List<IntArray> = indices

    /** The resulting triangles, optionally restricted by [filterPredicate]. */
    override fun triangles(filterPredicate: (Int, Int, Int) -> Boolean): List<Triangle> = indices
        .filter { (a, b, c) -> filterPredicate(a, b, c) }
        .map { (a, b, c) -> Triangle(points[c], points[b], points[a]) }

    override fun neighbors(pointIndex: Int): Sequence<Int> = neighborSets[pointIndex].asSequence()

    override fun voronoiDiagram(bounds: Rectangle): ConstrainedVoronoiDiagram = ConstrainedVoronoiDiagram(this, bounds)
}

/**
 * Computes the constrained Delaunay triangulation of this [Shape].
 *
 * @param interiorPoints additional, unconstrained points to include in the
 * triangulation, for example to refine/densify the mesh. Every point must lie
 * inside this [Shape].
 */
fun Shape.constrainedDelaunayTriangulation(
    interiorPoints: List<Vector2> = emptyList()
): ConstrainedDelaunayTriangulation = ConstrainedDelaunayTriangulation(this, interiorPoints)
