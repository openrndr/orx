package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.shape.bounds


fun Polygon2D.intersects(other: Polygon2D): Boolean {
    val bounds = bounds
    val otherBounds = other.bounds

    if (!bounds.intersects(otherBounds))
        return false

    // check if there are pairs of intersecting edges
    for (i in points.indices) {
        val a0 = points[i]
        val a1 = points[(i + 1) % points.size]

        for (j in other.points.indices) {
            val b0 = other.points[j]
            val b1 = other.points[(j + 1) % other.points.size]

            if (segmentsIntersect(a0, a1, b0, b1)) {
                return true
            }
        }
    }

    // if there are no intersecting edges, check if either polygon fully contains the other polygon
    if (this.points.isNotEmpty() && other.isPointInConcavePolygon(this.points[0])) {
        return true
    }

    if (other.points.isNotEmpty() && this.isPointInConcavePolygon(other.points[0])) {
        return true
    }

    return false
}

internal fun segmentsIntersect(p1: Vector2, p2: Vector2, p3: Vector2, p4: Vector2): Boolean {
    fun ccw(a: Vector2, b: Vector2, c: Vector2): Int {
        val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        return when {
            area > 0 -> 1
            area < 0 -> -1
            else -> 0
        }
    }

    fun onSegment(a: Vector2, b: Vector2, p: Vector2): Boolean {
        return p.x >= minOf(a.x, b.x) && p.x <= maxOf(a.x, b.x) &&
                p.y >= minOf(a.y, b.y) && p.y <= maxOf(a.y, b.y)
    }

    val ccw1 = ccw(p1, p2, p3)
    val ccw2 = ccw(p1, p2, p4)
    val ccw3 = ccw(p3, p4, p1)
    val ccw4 = ccw(p3, p4, p2)

    if (((ccw1 > 0 && ccw2 < 0) || (ccw1 < 0 && ccw2 > 0)) &&
        ((ccw3 > 0 && ccw4 < 0) || (ccw3 < 0 && ccw4 > 0))
    ) return true

    if (ccw1 == 0 && onSegment(p1, p2, p3)) return true
    if (ccw2 == 0 && onSegment(p1, p2, p4)) return true
    if (ccw3 == 0 && onSegment(p3, p4, p1)) return true
    if (ccw4 == 0 && onSegment(p3, p4, p2)) return true

    return false
}