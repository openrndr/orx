package org.openrndr.extra.mesh.rtree

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.query.verticesForEdge
import org.openrndr.extra.rtree.RTree
import org.openrndr.extra.rtree.squaredDistanceToSegment
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs


class RtreeDcelEdge2D(val dcel: Dcel, minEntries: Int = 2, maxEntries: Int = 4) {

    private fun Int.bounds(): Rectangle {
        val vertices = dcel.verticesForEdge(this)
        return vertices.map { dcel.vertices[it].position.xy }.bounds
    }

    private val rtree = RTree<Int>(minEntries, maxEntries) { it.bounds() }

    fun insert(edgeId: Int) = rtree.insert(edgeId)
    fun delete(edgeId: Int) = rtree.delete(edgeId)
    fun findInRange(area: Rectangle): EdgeList = EdgeList(rtree.findInRange(area))

    fun findKNearest(query: Vector2, k: Int): List<Int> {
        return rtree.findKNearest(query, k) { edge, q ->
            val vertices = dcel.verticesForEdge(edge)
            val p1 = dcel.vertices[vertices[0]].position.xy
            val p2 = dcel.vertices[vertices[1]].position.xy
            squaredDistanceToSegment(q, p1, p2)
        }
    }

    fun findContaining(query: Vector2, distanceTolerance: Double = 1e-6): List<Int> {
        val bounds = Rectangle.fromCenter(query, 1.0)

        return rtree.findInRange(bounds).filter {
            val vertices = dcel.verticesForEdge(it)
            val p0 = dcel.vertices[vertices[0]].position.xy
            val p1 = dcel.vertices[vertices[1]].position.xy
            distanceToSegment(query, p0, p1) < distanceTolerance
        }
    }


}

private fun distanceToSegment(p: Vector2, a: Vector2, b: Vector2): Double {
    val ab = b - a
    val ap = p - a

    val lengthSquared = ab.squaredLength

    // Segment is a point
    if (lengthSquared < 1e-10) {
        return ap.length
    }

    // Project p onto line ab, clamped to segment
    val t = (ap.dot(ab) / lengthSquared).coerceIn(0.0, 1.0)

    val closestPoint = a + ab * t
    return (p - closestPoint).length
}

private fun segmentsIntersect(a0: Vector2, a1: Vector2, b0: Vector2, b1: Vector2): Double? {
    val da = a1 - a0
    val db = b1 - b0
    val dc = b0 - a0

    val cross = da.x * db.y - da.y * db.x

    // Parallel or collinear
    if (abs(cross) < 1e-10) return null

    val t = (dc.x * db.y - dc.y * db.x) / cross
    val u = (dc.x * da.y - dc.y * da.x) / cross

    // Check if intersection point is within both segments
    return if (t in 0.0..1.0 && u in 0.0..1.0) t else null
}
    