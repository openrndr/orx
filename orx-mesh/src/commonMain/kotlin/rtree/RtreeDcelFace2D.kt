package org.openrndr.extra.mesh.rtree

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.convert.faceToPolygon3D
import org.openrndr.extra.mesh.dcel.convert.vertexListToPolygon3D
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.rtree.RTree
import org.openrndr.extra.rtree.bounds
import org.openrndr.extra.rtree.squaredDistanceToSegment
import org.openrndr.extra.shapes.polygon.Polygon2D
import org.openrndr.extra.shapes.polygon.intersects
import org.openrndr.extra.shapes.polygon.containsPoint
import org.openrndr.extra.shapes.polygon.xy
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds


class RtreeDcelFace2D(val dcel: Dcel, minEntries: Int = 2, maxEntries: Int = 4) {

    fun Int.bounds(): Rectangle {
        val vertices = dcel.verticesForFace(this)
        return vertices.map { dcel.vertices[it].position.xy }.bounds
    }

    private val rtree = RTree<Int>(minEntries, maxEntries) { it.bounds() }

    fun insert(faceId: Int) = rtree.insert(faceId)
    fun delete(faceId: Int) = rtree.delete(faceId)
    fun findInRange(area: Rectangle): FaceList = FaceList(rtree.findInRange(area))

    fun findKNearest(query: Vector2, k: Int): List<Int> {
        return rtree.findKNearest(query, k) { poly, q ->
            // Closest distance from point to polygon
            var minSqDist = Double.POSITIVE_INFINITY

            val vertices = dcel.verticesForFace(poly)
            for (i in vertices.indices) {
                val p1 = dcel.vertices[vertices[i]].position.xy
                val p2 = dcel.vertices[vertices[(i + 1) % vertices.size]].position.xy
                val d2 = squaredDistanceToSegment(q, p1, p2)
                if (d2 < minSqDist) minSqDist = d2
            }
            minSqDist
        }
    }

    fun findContaining(query: Vector2): List<Int> {
        val bounds = Rectangle.fromCenter(query, 1.0)

        return rtree.findInRange(bounds).filter {
            val p = dcel.faceToPolygon3D(it).xy
            p.containsPoint(query)
        }
    }

    fun findIntersecting(query: Polygon2D): List<Int> {
        return rtree.findInRange(query.bounds()).filter {
            val p = dcel.faceToPolygon3D(it).xy
            p.intersects(query, true)
        }
    }

    fun findIntersecting(vertexIds: List<Int>): List<Int> {
        val vertices = vertexIds.map { dcel.vertices[it].position.xy }
        val v = dcel.vertexListToPolygon3D(vertexIds).xy

        fun isSharedEdge(e0: Int, e1: Int): Boolean {
            val ii0 = vertexIds.indexOf(e0)
            if (ii0 == -1) return false
            val ii1 = vertexIds.indexOf(e1)
            if (ii1 == -1) return false
            return (ii0 + 1) % vertexIds.size == ii1 ||
                    (ii0 - 1).mod(vertexIds.size) == ii1
        }

        return rtree.findInRange(vertices.bounds).filter {
            val faceVertices = dcel.verticesForFace(it)

            for (j in 0 until vertexIds.size) {

                val j0 = vertexIds[j]
                val j1 = vertexIds[(j + 1) % vertexIds.size]
                val v0 = dcel.vertices[j0].position.xy
                val v1 = dcel.vertices[j1].position.xy

                inner@for (i in 0 until faceVertices.size) {
                    val i0 = faceVertices[i]
                    val i1 = faceVertices[(i + 1) % faceVertices.size]

                    if (isSharedEdge(i0, i1)) continue@inner

                    val u0 = dcel.vertices[i0].position.xy
                    val u1 = dcel.vertices[i1].position.xy

                    val t = segmentsIntersect(u0, u1, v0, v1)
                    if (t == null) continue@inner
                    else {
                        if ((t < 1E-6) /*&& i0 == j1*/)
                            continue@inner

                        if ((t > 1 - 1E-6) /*&& i1 == j0*/)
                            continue@inner

                        return@filter true
                    }
                }
            }
            val u = dcel.faceToPolygon3D(it).xy

//            for (i in 0 until v.size) {
//                val i0 = v[(i + 0) % v.size]
//                val i1 = v[(i + 1) % v.size]
//                val mid = (i0 + i1) / 2.0
//                if (u.contains(mid)) {
//                    return@filter true
//                }
//
//            }
            for (i in 0 until faceVertices.size) {
                val e0 = faceVertices[i]
                val e1 = faceVertices[(i + 1) % faceVertices.size]
                if (isSharedEdge(e0, e1)) continue

                val i0 = u[(i + 0) % faceVertices.size]
                val i1 = u[(i + 1) % faceVertices.size]
                val mid = (i0 + i1) / 2.0
                if (v.containsPoint(mid)) {
                    return@filter true
                }
            }
            false
        }
    }
}

private fun segmentsIntersect(a0: Vector2, a1: Vector2, b0: Vector2, b1: Vector2): Double? {
    val da = a1 - a0
    val db = b1 - b0
    val dc = b0 - a0

    val cross = da.x * db.y - da.y * db.x

    // Parallel or collinear
    if (kotlin.math.abs(cross) < 1e-10) return null

    val t = (dc.x * db.y - dc.y * db.x) / cross
    val u = (dc.x * da.y - dc.y * da.x) / cross

    // Check if intersection point is within both segments
    return if (t in 0.0..1.0 && u in 0.0..1.0) t else null
}
    