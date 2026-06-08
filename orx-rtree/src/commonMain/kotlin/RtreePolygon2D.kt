package org.openrndr.extra.rtree

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.extra.shapes.polygon.Polygon2D
import org.openrndr.extra.shapes.polygon.intersects
import org.openrndr.extra.shapes.polygon.containsPoint
import kotlin.math.*


fun Polygon2D.bounds(): Rectangle {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (v in this) {
        minX = min(minX, v.x)
        minY = min(minY, v.y)
        maxX = max(maxX, v.x)
        maxY = max(maxY, v.y)
    }
    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}


class RtreePolygon2D(minEntries: Int = 2, maxEntries: Int = 4) {
    private val rtree = RTree<Polygon2D>(minEntries, maxEntries) { it.bounds() }

    fun insert(polygon: Polygon2D) = rtree.insert(polygon)
    fun delete(polygon: Polygon2D) = rtree.delete(polygon)

    fun findInRange(area: Rectangle): List<Polygon2D> = rtree.findInRange(area)

    fun findKNearest(query: Vector2, k: Int): List<Polygon2D> {
        return rtree.findKNearest(query, k) { poly, q ->
            // Closest distance from point to polygon
            var minSqDist = Double.POSITIVE_INFINITY
            for (i in poly.indices) {
                val p1 = poly[i]
                val p2 = poly[(i + 1) % poly.size]
                val d2 = squaredDistanceToSegment(q, p1, p2)
                if (d2 < minSqDist) minSqDist = d2
            }
            minSqDist
        }
    }

    fun findIntersecting(query: Polygon2D): List<Polygon2D> {
        return rtree.findInRange(query.bounds()).filter { polygon -> polygon.intersects(query, true) }
    }

    fun findContaining(query: Vector2): List<Polygon2D> {
        val result =  rtree.findInRange(Rectangle.fromCenter(query, 1.0, 1.0))
        val result2 = result.filter { polygon -> polygon.containsPoint(query) }
        return result2
    }
}

fun squaredDistanceToSegment(p: Vector2, a: Vector2, b: Vector2): Double {
    val ab = b - a
    val ap = p - a
    val bp = p - b
    val e = ap.dot(ab)
    if (e <= 0) return ap.squaredLength
    val f = ab.dot(ab)
    if (e >= f) return bp.squaredLength
    return ap.squaredLength - e * e / f
}


