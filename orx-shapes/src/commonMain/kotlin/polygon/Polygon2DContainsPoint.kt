package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2

fun Polygon2D.containsPoint(point: Vector2): Boolean {
    var intersections = 0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]

        if (((p1.y <= point.y && point.y < p2.y) || (p2.y <= point.y && point.y < p1.y))) {
             val intersectX = (p2.x - p1.x) * (point.y - p1.y) / (p2.y - p1.y) + p1.x
             if (point.x < intersectX) {
                 intersections++
             }
        }
    }
    return intersections % 2 != 0
}
