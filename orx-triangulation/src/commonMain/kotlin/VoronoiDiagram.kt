package org.openrndr.extra.triangulation

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds

class VoronoiDiagram(
    val delaunayTriangulation: DelaunayTriangulation,
    override val bounds: Rectangle
) : AbstractVoronoiDiagram {
    override val triangulation: AbstractDelaunayTriangulation get() = delaunayTriangulation

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

    override fun cellPolygon(i: Int): ShapeContour {
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

    override fun neighbors(cellIndex: Int): Sequence<Int> {
        return voronoi.neighbors(cellIndex)
    }

    /**
     * The edges dual to the interior halfedges of the Delaunay triangulation (connecting the
     * circumcenters of adjacent triangles), restricted to those with both endpoints strictly
     * inside [bounds]. Since [bounds] is convex such an edge never touches the boundary.
     *
     * Circumcenters that (nearly) coincide, as happens for co-circular sites, are merged into
     * a single vertex and the resulting zero-length edges are dropped.
     */
    override fun internalEdges(): Pair<List<Vector2>, List<IntArray>> {
        val delaunay = delaunayTriangulation.delaunay
        val halfedges = delaunay.halfedges
        val centers = circumcenters
        val triangleCount = delaunay.triangles.size / 3
        if (triangleCount == 0) return Pair(emptyList(), emptyList())

        val epsilon = 1E-9 * maxOf(bounds.width, bounds.height, 1.0)

        // merge (nearly) coincident circumcenters of adjacent triangles
        val parent = IntArray(triangleCount) { it }
        fun find(i: Int): Int {
            var r = i
            while (parent[r] != r) r = parent[r]
            var c = i
            while (parent[c] != r) {
                val next = parent[c]
                parent[c] = r
                c = next
            }
            return r
        }
        for (e in halfedges.indices) {
            val o = halfedges[e]
            if (o < e) continue
            val a = e / 3
            val b = o / 3
            if (centers[a].distanceTo(centers[b]) <= epsilon) {
                parent[find(a)] = find(b)
            }
        }

        fun strictlyInside(p: Vector2) =
            p.x > bounds.x && p.x < bounds.x + bounds.width && p.y > bounds.y && p.y < bounds.y + bounds.height

        val vertices = mutableListOf<Vector2>()
        val vertexIndex = IntArray(triangleCount) { -1 }
        val edges = mutableListOf<IntArray>()
        val seen = mutableSetOf<Long>()

        for (e in halfedges.indices) {
            val o = halfedges[e]
            if (o < e) continue
            val a = find(e / 3)
            val b = find(o / 3)
            if (a == b) continue
            if (!strictlyInside(centers[a]) || !strictlyInside(centers[b])) continue
            if (!seen.add(minOf(a, b).toLong() * triangleCount + maxOf(a, b))) continue

            if (vertexIndex[a] == -1) { vertexIndex[a] = vertices.size; vertices.add(centers[a]) }
            if (vertexIndex[b] == -1) { vertexIndex[b] = vertices.size; vertices.add(centers[b]) }
            edges.add(intArrayOf(vertexIndex[a], vertexIndex[b]))
        }
        return Pair(vertices, edges)
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