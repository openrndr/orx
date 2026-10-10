package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

/**
 * Common surface for a Voronoi diagram generated from an [AbstractDelaunayTriangulation]'s
 * [AbstractDelaunayTriangulation.points] (the Voronoi sites), with cells clipped to [bounds].
 */
interface AbstractVoronoiDiagram {

    /** The triangulation whose points are the Voronoi sites. */
    val triangulation: AbstractDelaunayTriangulation

    /** The rectangular region cells are clipped to. */
    val bounds: Rectangle

    /** The Voronoi cell polygon for site [i] (index into [triangulation]'s points), clipped to [bounds]. */
    fun cellPolygon(i: Int): ShapeContour

    /** The Voronoi cell polygons for every site, in point order. */
    fun cellPolygons(): List<ShapeContour> = triangulation.points.indices.map { cellPolygon(it) }

    /** The (signed) area of cell [i]. */
    fun cellArea(i: Int, contour: ShapeContour = cellPolygon(i)): Double {
        val segments = contour.segments
        var sum = 0.0
        for (j in segments.indices) {
            val v0 = segments[j].start
            val v1 = segments[(j + 1).mod(segments.size)].start
            sum += v0.x * v1.y - v1.x * v0.y
        }
        return sum / 2.0
    }

    /** The centroid of cell [i]. */
    fun cellCentroid(i: Int, contour: ShapeContour = cellPolygon(i)): Vector2 {
        val segments = cellPolygon(i).segments
        var cx = 0.0
        var cy = 0.0
        for (j in segments.indices) {
            val v0 = segments[j].start
            val v1 = segments[(j + 1).mod(segments.size)].start
            cx += (v0.x + v1.x) * (v0.x * v1.y - v1.x * v0.y)
            cy += (v0.y + v1.y) * (v0.x * v1.y - v1.x * v0.y)
        }
        val a = cellArea(i, contour) * 6.0
        cx /= a
        cy /= a
        return Vector2(cx, cy)
    }

    /** The centroids of every cell, in point order. */
    fun cellCentroids(): List<Vector2> = triangulation.points.indices.map { cellCentroid(it) }

    /**
     * All edges of the diagram that neither lie on nor touch the boundary of the diagram.
     *
     * @return a pair of the edge vertices and the edges as index pairs into those vertices.
     * Every vertex is referenced by at least one edge; vertices shared by several edges appear once.
     */
    fun internalEdges(): Pair<List<Vector2>, List<IntArray>>

    /** Indices of the cells adjacent to cell [cellIndex]. */
    fun neighbors(cellIndex: Int): Sequence<Int>
}
