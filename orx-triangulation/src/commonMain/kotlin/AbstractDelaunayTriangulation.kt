package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.bounds

/**
 * Common surface shared by [DelaunayTriangulation] and [ConstrainedDelaunayTriangulation]:
 * a set of vertex [points] and a triangulation of (some subset of) them.
 */
interface AbstractDelaunayTriangulation {

    /** The vertex positions referenced by [triangleIndices]. */
    val points: List<Vector2>

    /** The triangles as index triples into [points], in counter-clockwise order. */
    fun triangleIndices(): List<IntArray>

    /**
     * The triangles, optionally restricted to those whose vertex indices (in
     * counter-clockwise order) satisfy [filterPredicate].
     */
    fun triangles(filterPredicate: (Int, Int, Int) -> Boolean = { _, _, _ -> true }): List<Triangle>

    /** Indices into [points] of the nodes adjacent to [pointIndex] in the triangulation. */
    fun neighbors(pointIndex: Int): Sequence<Int>

    /** The positions of the nodes adjacent to [pointIndex] in the triangulation. */
    fun neighborPoints(pointIndex: Int): List<Vector2> = neighbors(pointIndex).map { points[it] }.toList()

    /** Index into [points] of the point nearest to [query]. */
    fun nearest(query: Vector2): Int {
        var best = 0
        var bestSquaredDistance = Double.POSITIVE_INFINITY
        for (i in points.indices) {
            val d = (points[i] - query).squaredLength
            if (d < bestSquaredDistance) {
                bestSquaredDistance = d
                best = i
            }
        }
        return best
    }

    /** The point nearest to [query]. */
    fun nearestPoint(query: Vector2): Vector2 = points[nearest(query)]

    /** The Voronoi diagram dual to this triangulation, with cells clipped to [bounds]. */
    fun voronoiDiagram(bounds: Rectangle = points.bounds): AbstractVoronoiDiagram
}
