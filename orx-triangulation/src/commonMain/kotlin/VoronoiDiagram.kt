package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds

class VoronoiDiagram(val delaunayTriangulation: DelaunayTriangulation, val bounds: Rectangle) {
    private val voronoi = Voronoi(delaunayTriangulation.delaunay, bounds)

    val vectors by lazy {
        voronoi.vectors.toList().windowed(2, 2).map {
            Vector2(it[0], it[1])
        }
    }

    val circumcenters by lazy {
        voronoi.circumcenters.toList().windowed(2, 2).map {
            Vector2(it[0], it[1])
        }
    }

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

    fun cellCentroids() = (delaunayTriangulation.points.indices).map {
        cellCentroid(it)
    }

    fun cellPolygon(i: Int): ShapeContour {
        val points = voronoi.clip(i)

        if (points == null || points.isEmpty()) return ShapeContour.EMPTY

        val polygon = mutableListOf(Vector2(points[0], points[1]))
        var n = points.size

        while (n > 1 && points[0] == points[n - 2] && points[1] == points[n - 1]) n -= 2

        for (idx in 2 until n step 2) {
            if (points[idx] != points[idx - 2] || points[idx + 1] != points[idx - 1]) {
                polygon.add(Vector2(points[idx], points[idx + 1]))
            }
        }
        return ShapeContour.fromPoints(polygon, true)
    }

    fun cellPolygons(): List<ShapeContour> {
        val points = delaunayTriangulation.points
        return (points.indices).map {
            cellPolygon(it)
        }
    }

    fun neighbors(cellIndex: Int): Sequence<Int> {
        return voronoi.neighbors(cellIndex)
    }
}

/**
 * Generates a Voronoi diagram based on the points in the list and the provided bounds.
 *
 * @param bounds The rectangular bounds within which the Voronoi diagram is generated. Defaults to the bounds of the point list.
 * @return A VoronoiDiagram object representing the calculated Voronoi diagram.
 */
fun List<Vector2>.voronoiDiagram(bounds: Rectangle = this.bounds): VoronoiDiagram {
    val d = this.delaunayTriangulation()
    return d.voronoiDiagram(bounds)
}