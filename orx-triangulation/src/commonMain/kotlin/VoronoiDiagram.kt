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

fun List<Vector2>.voronoiDiagram(bounds: Rectangle = this.bounds): VoronoiDiagram {
    val d = this.delaunayTriangulation()
    return d.voronoiDiagram(bounds)
}