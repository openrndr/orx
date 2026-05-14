package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName
import kotlin.math.abs

fun Polygon2D.isConvex(): Boolean = isConvexPolygon(points)

fun isConvexPolygon(points: List<Vector2>): Boolean {
    if (points.size < 3) return true
    var sign = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val p3 = points[(i + 2) % points.size]
        val cp = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x)
        if (cp != 0.0) {
            if (sign == 0.0) {
                sign = cp
            } else if (sign * cp < 0) {
                return false
            }
        }
    }
    return true
}

@JvmName("isConvexPolygon3")
fun isConvexPolygon(points: List<Vector3>): Boolean {

    val n = points.size

    var referenceNormal = Vector3.ZERO

    for (i in 0 until n) {
        val p0 = points[i]
        val p1 = points[(i + 1) % n]
        val p2 = points[(i + 2) % n]

        val v0 = p1 - p0
        val v1 = p2 - p1

        val cross = v0.cross(v1)
        if (cross.length > 1e-6) {
            if (referenceNormal == Vector3.ZERO) {
                referenceNormal = cross
            } else {
                if (referenceNormal.dot(cross) < -1e-6) {
                    return false
                }
            }
        }
    }
    return true
}

fun isPointInConvexPolygon(points: List<Vector2>, q: Vector2): Boolean {
    if (points.isEmpty()) return false
    if (points.size == 1) return points[0] == q
    if (points.size == 2) {
        // Point on line segment check could be added here, but usually polygons have >= 3 points
        val p1 = points[0]
        val p2 = points[1]
        val dotProduct = (q.x - p1.x) * (p2.x - p1.x) + (q.y - p1.y) * (p2.y - p1.y)
        if (dotProduct < 0) return false
        val squaredLength = (p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)
        if (dotProduct > squaredLength) return false
        val crossProduct = (p2.x - p1.x) * (q.y - p1.y) - (p2.y - p1.y) * (q.x - p1.x)
        return crossProduct == 0.0
    }

    var sign = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val cp = (p2.x - p1.x) * (q.y - p1.y) - (p2.y - p1.y) * (q.x - p1.x)
        if (cp != 0.0) {
            if (sign == 0.0) {
                sign = cp
            } else if (sign * cp < 0) {
                return false
            }
        }
    }
    return true
}

@JvmName("isPointInConvexPolygon3")
fun isPointInConvexPolygon(points: List<Vector3>, q: Vector3): Boolean {
    if (points.isEmpty()) return false
    if (points.size == 1) return points[0] == q
    if (points.size == 2) {
        val p1 = points[0]
        val p2 = points[1]
        val v = p2 - p1
        val w = q - p1
        val dotProduct = w.dot(v)
        if (dotProduct < 0) return false
        val squaredLength = v.dot(v)
        if (dotProduct > squaredLength) return false
        val crossProduct = v.cross(w)
        return crossProduct.length < 1e-6
    }

    var normal = Vector3.ZERO
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val p3 = points[(i + 2) % points.size]
        val v1 = p2 - p1
        val v2 = p3 - p2
        val cross = v1.cross(v2)
        if (cross.length > 1e-6) {
            normal = cross.normalized
            break
        }
    }

    if (normal == Vector3.ZERO) {
        // All points are collinear, treat as line segment
        // This is a bit simplified, but covers the case of a degenerate polygon
        var minDot = Double.POSITIVE_INFINITY
        var maxDot = Double.NEGATIVE_INFINITY
        val p0 = points[0]
        val v = (points.firstOrNull { it != p0 } ?: return p0 == q) - p0
        val vn = v.normalized
        for (p in points) {
            val d = (p - p0).dot(vn)
            minDot = minOf(minDot, d)
            maxDot = maxOf(maxDot, d)
        }
        val qd = (q - p0).dot(vn)
        val qp = (q - p0)
        return qp.cross(vn).length < 1e-6 && qd >= minDot && qd <= maxDot
    }

    // Check if q is coplanar
    val vq = q - points[0]
    if (abs(vq.dot(normal)) > 1e-6) return false

    var sign = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        val edge = p2 - p1
        val toQ = q - p1
        val cp = edge.cross(toQ)
        val dot = cp.dot(normal)
        if (abs(dot) > 1e-9) {
            if (sign == 0.0) {
                sign = dot
            } else if (sign * dot < 0) {
                return false
            }
        }
    }
    return true
}

fun convexPolygonCenter(points: List<Vector2>): Vector2 {
    if (points.isEmpty()) return Vector2.ZERO
    return points.reduce { acc, vector2 -> acc + vector2 } / points.size.toDouble()
}

@JvmName("convexPolygonCenter3")
fun convexPolygonCenter(points: List<Vector3>): Vector3 {
    if (points.isEmpty()) return Vector3.ZERO
    return points.reduce { acc, vector3 -> acc + vector3 } / points.size.toDouble()
}