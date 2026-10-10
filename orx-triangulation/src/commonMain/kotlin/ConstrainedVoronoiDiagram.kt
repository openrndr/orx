package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import org.openrndr.shape.intersection

/**
 * The Voronoi diagram dual to a [ConstrainedDelaunayTriangulation]'s points, with every
 * cell clipped both to [bounds] and to the triangulation's
 * [ConstrainedDelaunayTriangulation.shape] -- so cells never extend outside a non-convex
 * outer contour or across a hole boundary.
 *
 * This delegates the heavy lifting (circumcenters, exterior-cell rays, rectangle clipping)
 * to an ordinary, unconstrained [VoronoiDiagram] of the same points, then additionally
 * intersects each resulting cell with [shape]. When that intersection yields more than one
 * polygon (a cell straddling a hole, for example), the largest (by area) is kept, since
 * [cellPolygon] returns a single [ShapeContour].
 */
class ConstrainedVoronoiDiagram(
    val constrainedDelaunayTriangulation: ConstrainedDelaunayTriangulation,
    override val bounds: Rectangle
) : AbstractVoronoiDiagram {

    override val triangulation: AbstractDelaunayTriangulation get() = constrainedDelaunayTriangulation

    private val shape get() = constrainedDelaunayTriangulation.shape
    private val unconstrained = DelaunayTriangulation(constrainedDelaunayTriangulation.points).voronoiDiagram(bounds)

    override fun cellPolygon(i: Int): ShapeContour {
        val raw = unconstrained.cellPolygon(i)
        if (raw.segments.isEmpty()) return raw

        val clipped = raw.shape.intersection(shape)
        return clipped.contours.maxByOrNull { kotlin.math.abs(Shape(listOf(it)).area) } ?: ShapeContour.EMPTY
    }

    /**
     * The internal edges of the unconstrained diagram (see [VoronoiDiagram.internalEdges])
     * that lie strictly inside [shape]: both endpoints inside the shape and away from its
     * contours, and the edge not crossing any contour.
     */
    override fun internalEdges(): Pair<List<Vector2>, List<IntArray>> {
        val (candidateVertices, candidateEdges) = unconstrained.internalEdges()
        val boundary = shape.contours.flatMap { contour -> contour.segments.map { it.start to it.end } }
        val epsilon = 1E-9 * maxOf(bounds.width, bounds.height, 1.0)

        val vertexInside = BooleanArray(candidateVertices.size) { i ->
            val p = candidateVertices[i]
            p in shape && boundary.none { (a, b) -> segmentDistance(p, a, b) <= epsilon }
        }

        val vertices = mutableListOf<Vector2>()
        val remap = IntArray(candidateVertices.size) { -1 }
        val edges = mutableListOf<IntArray>()
        for ((i, j) in candidateEdges) {
            if (!vertexInside[i] || !vertexInside[j]) continue
            val p = candidateVertices[i]
            val q = candidateVertices[j]
            if (boundary.any { (a, b) -> segmentsIntersect(p, q, a, b) }) continue

            if (remap[i] == -1) { remap[i] = vertices.size; vertices.add(p) }
            if (remap[j] == -1) { remap[j] = vertices.size; vertices.add(q) }
            edges.add(intArrayOf(remap[i], remap[j]))
        }
        return Pair(vertices, edges)
    }

    /** Two sites are neighbors here iff they share an edge in the constrained triangulation. */
    override fun neighbors(cellIndex: Int): Sequence<Int> = constrainedDelaunayTriangulation.neighbors(cellIndex)
}

private fun segmentDistance(p: Vector2, a: Vector2, b: Vector2): Double {
    val ab = b - a
    val lengthSquared = ab.squaredLength
    if (lengthSquared == 0.0) return p.distanceTo(a)
    val t = ((p - a).dot(ab) / lengthSquared).coerceIn(0.0, 1.0)
    return p.distanceTo(a + ab * t)
}

private fun cross(o: Vector2, a: Vector2, b: Vector2): Double = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

/** Whether segments pq and ab intersect, assuming neither p nor q lies on ab. */
private fun segmentsIntersect(p: Vector2, q: Vector2, a: Vector2, b: Vector2): Boolean {
    val d1 = cross(a, b, p)
    val d2 = cross(a, b, q)
    val d3 = cross(p, q, a)
    val d4 = cross(p, q, b)
    return ((d1 > 0) != (d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0) || d3 == 0.0 || d4 == 0.0)
}
