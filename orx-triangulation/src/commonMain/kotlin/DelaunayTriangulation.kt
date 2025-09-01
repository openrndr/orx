package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.contour
import org.openrndr.shape.contours

/**
 * Kotlin/OPENRNDR idiomatic interface to `Delaunay`
 */
class DelaunayTriangulation(val points: List<Vector2>) {
    val delaunay: Delaunay = Delaunay.from(points)

    fun voronoiDiagram(bounds: Rectangle) = VoronoiDiagram(this, bounds)

    fun neighbors(pointIndex: Int): Sequence<Int> {
        return delaunay.neighbors(pointIndex)
    }

    fun neighborPoints(pointIndex: Int): List<Vector2> {
        return neighbors(pointIndex).map { points[it] }.toList()
    }

    fun triangleIndices(): List<IntArray> {
        val list = mutableListOf<IntArray>()
        for (i in delaunay.triangles.indices step 3) {
            list.add(
                intArrayOf(
                    delaunay.triangles[i],
                    delaunay.triangles[i + 1],
                    delaunay.triangles[i + 2]
                )
            )
        }
        return list
    }

    fun triangles(filterPredicate: (Int, Int, Int) -> Boolean = { _, _, _ -> true }): List<Triangle> {
        val list = mutableListOf<Triangle>()

        for (i in delaunay.triangles.indices step 3) {
            val t0 = delaunay.triangles[i]
            val t1 = delaunay.triangles[i + 1]
            val t2 = delaunay.triangles[i + 2]

            // originally they are defined *counterclockwise*
            if (filterPredicate(t2, t1, t0)) {
                val p1 = points[t0]
                val p2 = points[t1]
                val p3 = points[t2]
                list.add(Triangle(p3, p2, p1))
            }
        }
        return list
    }

    // Inner edges of the delaunay triangulation (without hull)
    fun halfedges() = contours {
        for (i in delaunay.halfedges.indices) {
            val j = delaunay.halfedges[i]

            if (j < i) continue
            val ti = delaunay.triangles[i]
            val tj = delaunay.triangles[j]

            moveTo(points[ti])
            lineTo(points[tj])
        }
    }

    fun hull() = contour {
        for (h in delaunay.hull) {
            moveOrLineTo(points[h])
        }
        close()
    }

    fun nearest(query: Vector2): Int = delaunay.find(query.x, query.y)

    fun nearestPoint(query: Vector2): Vector2 = points[nearest(query)]
}

/**
 * Computes the Delaunay triangulation for the list of 2D points.
 *
 * The Delaunay triangulation is a triangulation of a set of points such that
 * no point is inside the circumcircle of any triangle. It maximizes the minimum
 * angle of all the angles in the triangles, avoiding skinny triangles.
 *
 * @return A DelaunayTriangulation object representing the triangulation of the given points.
 */
fun List<Vector2>.delaunayTriangulation(): DelaunayTriangulation {
    return DelaunayTriangulation(this)
}
