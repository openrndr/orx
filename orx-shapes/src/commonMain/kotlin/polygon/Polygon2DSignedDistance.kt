package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import kotlin.math.absoluteValue
import kotlin.math.sqrt

private fun squaredDistanceToSegment(p: Vector2, a: Vector2, b: Vector2): Double {
    val ab = b - a
    val ap = p - a
    val bp = p - b
    val e = ap.dot(ab)
    if (e <= 0.0) return ap.squaredLength
    val f = ab.dot(ab)
    if (e >= f) return bp.squaredLength
    return (ap.squaredLength - e * e / f).coerceAtLeast(0.0)
}

fun Polygon2D.distance(point: Vector2): Double {
    if (points.isEmpty()) return Double.POSITIVE_INFINITY

    var minSqDist = Double.POSITIVE_INFINITY
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val d2 = squaredDistanceToSegment(point, p1, p2)
        if (d2 < minSqDist) {
            minSqDist = d2
        }
    }

    val dist = sqrt(minSqDist)
    return dist
}
/**
 * Calculate the signed distance from [point] to the polygon.
 * Points inside the polygon have a negative distance, points outside have a positive distance.
 */
fun Polygon2D.signedDistance(point: Vector2): Double {
    if (points.isEmpty()) return Double.POSITIVE_INFINITY

    var minSqDist = Double.POSITIVE_INFINITY
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val d2 = squaredDistanceToSegment(point, p1, p2)
        if (d2 < minSqDist) {
            minSqDist = d2
        }
    }

    val dist = sqrt(minSqDist)
    return if (containsPoint(point)) -dist else dist
}