package org.openrndr.extra.shapes.kernel

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName
import kotlin.math.abs

fun findPolygonNormal(points: List<Vector3>): Vector3 {
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
    return normal
}

fun findKernel(points: List<Vector2>): List<Vector2> {
    if (points.size < 3) return emptyList()

    // Sutherland-Hodgman algorithm to find the intersection of all half-planes
    // defined by the polygon edges.

    // 1. Determine orientation (assuming simple polygon)
    var area = 0.0
    for (i in points.indices) {
        val p1 = points[i]
        val p2 = points[(i + 1) % points.size]
        area += (p1.x * p2.y - p2.x * p1.y)
    }
    val ccw = area > 0

    // 2. Initial "kernel" is the bounding box of the polygon (expanded slightly)
    var minX = points[0].x
    var maxX = points[0].x
    var minY = points[0].y
    var maxY = points[0].y
    for (p in points) {
        if (p.x < minX) minX = p.x
        if (p.x > maxX) maxX = p.x
        if (p.y < minY) minY = p.y
        if (p.y > maxY) maxY = p.y
    }

    val dx = maxX - minX
    val dy = maxY - minY
    minX -= dx * 0.1
    maxX += dx * 0.1
    minY -= dy * 0.1
    maxY += dy * 0.1

    var kernel = listOf(
        Vector2(minX, minY),
        Vector2(maxX, minY),
        Vector2(maxX, maxY),
        Vector2(minX, maxY)
    )

    // 3. For each edge, clip the kernel
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        if (a.distanceTo(b) < 1e-10) continue

        val newKernel = mutableListOf<Vector2>()

        // The edge (a, b) defines a half-plane.
        // For CCW polygon, the interior is to the left of (a, b).
        // A point p is inside if cross product (b-a) x (p-a) >= 0.

        fun isInside(p: Vector2): Boolean {
            val cp = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
            return if (ccw) cp >= -1e-10 else cp <= 1e-10
        }

        fun intersect(p1: Vector2, p2: Vector2): Vector2 {
            val x1 = a.x
            val y1 = a.y
            val x2 = b.x
            val y2 = b.y

            val x3 = p1.x
            val y3 = p1.y
            val x4 = p2.x
            val y4 = p2.y

            val den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
            val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
            return Vector2(x1 + t * (x2 - x1), y1 + t * (y2 - y1))
        }

        if (kernel.isEmpty()) return emptyList()

        for (j in kernel.indices) {
            val p1 = kernel[j]
            val p2 = kernel[(j + 1) % kernel.size]

            val in1 = isInside(p1)
            val in2 = isInside(p2)

            if (in1 && in2) {
                newKernel.add(p2)
            } else if (in1 && !in2) {
                newKernel.add(intersect(p1, p2))
            } else if (!in1 && in2) {
                newKernel.add(intersect(p1, p2))
                newKernel.add(p2)
            }
        }
        kernel = newKernel
    }

    return if (kernel.size < 3) emptyList() else kernel
}

@JvmName("findKernel3")
fun findKernel(points: List<Vector3>): List<Vector3> {
    if (points.size < 3) return emptyList()

   val normal = findPolygonNormal(points)

    if (normal == Vector3.ZERO) {
        return emptyList()
    }

    val bitangent1 = if (abs(normal.z) < 0.9) {
        normal.cross(Vector3.UNIT_Z).normalized
    } else {
        normal.cross(Vector3.UNIT_Y).normalized
    }
    val bitangent2 = normal.cross(bitangent1).normalized

    val origin = points[0]
    val points2D = points.map {
        val v = it - origin
        Vector2(v.dot(bitangent1), v.dot(bitangent2))
    }

    var area = 0.0
    for (i in points2D.indices) {
        val p1 = points2D[i]
        val p2 = points2D[(i + 1) % points2D.size]
        area += p1.x * p2.y - p2.x * p1.y
    }
    val points2DOrdered = if (area < 0) points2D.reversed() else points2D

    val kernel2D = findKernel(points2DOrdered)

    return kernel2D.map {
        origin + bitangent1 * it.x + bitangent2 * it.y
    }
}