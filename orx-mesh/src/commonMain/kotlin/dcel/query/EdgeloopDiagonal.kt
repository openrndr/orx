package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

fun Dcel.isEdgeloopDiagonal(edgeId0: Int, edgeId1: Int): Boolean {
    if (edgeId0 == edgeId1) return false
    val faceId = halfEdges[edgeId0].face
    if (faceId == -1 || halfEdges[edgeId1].face != faceId) return false

    val loop = edgeLoopIndices(edgeId0)
    if (edgeId1 !in loop) return false

    val i0 = loop.indexOf(edgeId0)
    val i1 = loop.indexOf(edgeId1)

    // Check if they are neighbors in the loop
    if (i0 == (i1 + 1) % loop.size || i1 == (i0 + 1) % loop.size) {
        return false
    }

    val positions = loop.map { vertices[halfEdges[it].vertex].position }

    // Find loop normal and check if it's planar
    var areaVector = Vector3.ZERO
    for (i in positions.indices) {
        val v0 = positions[i]
        val v1 = positions[(i + 1) % positions.size]
        areaVector += v0.cross(v1)
    }

    if (areaVector.length < 1e-12) return false
    val normal = areaVector.normalized

    // Project points to 2D
    val tangent = if (abs(normal.z) < 0.9) Vector3.UNIT_Z else Vector3.UNIT_Y
    val bitangent = normal.cross(tangent).normalized
    val finalTangent = bitangent.cross(normal).normalized

    fun project(p: Vector3): Vector2 {
        return Vector2(p.dot(finalTangent), p.dot(bitangent))
    }

    val positions2d = positions.map { project(it) }
    val p0 = positions2d[i0]
    val p1 = positions2d[i1]

    // Check for intersections with loop edges
    for (i in positions2d.indices) {
        val e0 = positions2d[i]
        val e1 = positions2d[(i + 1) % positions2d.size]

        // If the edge shares a vertex with the diagonal, it's not a crossing intersection
        val sharesVertex = i == i0 || i == i1 || (i + 1) % positions2d.size == i0 || (i + 1) % positions2d.size == i1

        if (sharesVertex) {
            continue
        }

        if (segmentsIntersect(p0, p1, e0, e1, includeEndpoints = true)) {
            return false
        }
    }

    // Check if any other vertex in the loop lies on the diagonal segment
    for (i in positions2d.indices) {
        if (i == i0 || i == i1) continue
        val p = positions2d[i]
        if (onSegment(p0, p1, p)) {
            return false
        }
    }

    // Midpoint check to ensure it's inside
    val mid = (positions[i0] + positions[i1]) * 0.5
    val p2d = project(mid)

    // Ray casting algorithm for point-in-polygon using the same local projection
    var inside = false
    var j = positions2d.size - 1
    for (i in positions2d.indices) {
        val pi = positions2d[i]
        val pj = positions2d[j]

        if (((pi.y > p2d.y) != (pj.y > p2d.y)) &&
            (p2d.x < (pj.x - pi.x) * (p2d.y - pi.y) / (pj.y - pi.y) + pi.x)
        ) {
            inside = !inside
        }
        j = i
    }

    if (!inside) {
        return false
    }

    return true
}

fun Dcel.isEdgeloopChord(edgeId0: Int, edgeT0: Double, edgeId1: Int, edgeT1: Double): Boolean {
    if (edgeId0 == edgeId1 && abs(edgeT0 - edgeT1) < 1e-9) return false
    val faceId = halfEdges[edgeId0].face
    if (faceId == -1 || halfEdges[edgeId1].face != faceId) return false

    val loop = edgeLoopIndices(edgeId0)
    if (edgeId1 !in loop) return false

    val positions = loop.map { vertices[halfEdges[it].vertex].position }

    // Find loop normal and check if it's planar
    var areaVector = Vector3.ZERO
    for (i in positions.indices) {
        val v0 = positions[i]
        val v1 = positions[(i + 1) % positions.size]
        areaVector += v0.cross(v1)
    }

    if (areaVector.length < 1e-12) return false
    val normal = areaVector.normalized

    // Project points to 2D
    val tangent = if (abs(normal.z) < 0.9) Vector3.UNIT_Z else Vector3.UNIT_Y
    val bitangent = normal.cross(tangent).normalized
    val finalTangent = bitangent.cross(normal).normalized

    fun project(p: Vector3): Vector2 {
        return Vector2(p.dot(finalTangent), p.dot(bitangent))
    }

    val positions2d = positions.map { project(it) }

    val pos0 = edgePosition(edgeId0, edgeT0)
    val pos1 = edgePosition(edgeId1, edgeT1)
    val p0 = project(pos0)
    val p1 = project(pos1)

    // Check for intersections with loop edges
    for (i in loop.indices) {
        val loopEdgeId = loop[i]
        val e0 = positions2d[i]
        val e1 = positions2d[(i + 1) % positions2d.size]

        if (segmentsIntersect(p0, p1, e0, e1, includeEndpoints = false)) {
            return false
        } else {
             // segmentsIntersect(includeEndpoints=false) returns false if they only touch at endpoints.
             // We still need to check if the chord is collinear with an edge that it's NOT supposed to be on.
             if (onSegment(e0, e1, p0) && onSegment(e0, e1, p1)) {
                 if (loopEdgeId != edgeId0 && loopEdgeId != edgeId1) {
                     return false
                 }
             }
        }
    }

    // Check if any vertex in the loop lies on the chord segment (excluding endpoints if they coincide)
    for (i in positions2d.indices) {
        val p = positions2d[i]
        if ((p - p0).length > 1e-9 && (p - p1).length > 1e-9) {
            if (onSegment(p0, p1, p)) {
                return false
            }
        }
    }

    // Midpoint check to ensure it's inside
    if (edgeId0 != edgeId1 || abs(edgeT0 - edgeT1) > 1e-9) {
        val mid = (pos0 + pos1) * 0.5
        val p2d = project(mid)

        // Ray casting algorithm for point-in-polygon using the same local projection
        var inside = false
        var j = positions2d.size - 1
        for (i in positions2d.indices) {
            val pi = positions2d[i]
            val pj = positions2d[j]

            if (((pi.y > p2d.y) != (pj.y > p2d.y)) &&
                (p2d.x < (pj.x - pi.x) * (p2d.y - pi.y) / (pj.y - pi.y) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }

        if (!inside) {
            return false
        }
    }

    return true
}


private fun segmentsIntersect(
    p1: Vector2, p2: Vector2,
    p3: Vector2, p4: Vector2,
    includeEndpoints: Boolean = true
): Boolean {
    fun ccw(a: Vector2, b: Vector2, c: Vector2): Int {
        val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        return when {
            area > 1e-9 -> 1
            area < -1e-9 -> -1
            else -> 0
        }
    }

    val ccw1 = ccw(p1, p2, p3)
    val ccw2 = ccw(p1, p2, p4)
    val ccw3 = ccw(p3, p4, p1)
    val ccw4 = ccw(p3, p4, p2)

    if (((ccw1 > 0 && ccw2 < 0) || (ccw1 < 0 && ccw2 > 0)) &&
        ((ccw3 > 0 && ccw4 < 0) || (ccw3 < 0 && ccw4 > 0))
    ) return true

    if (includeEndpoints) {
        if (ccw1 == 0 && onSegment(p1, p2, p3)) return true
        if (ccw2 == 0 && onSegment(p1, p2, p4)) return true
        if (ccw3 == 0 && onSegment(p3, p4, p1)) return true
        if (ccw4 == 0 && onSegment(p3, p4, p2)) return true
    }

    return false
}

private fun onSegment(a: Vector2, b: Vector2, p: Vector2): Boolean {
    val cross = (p.y - a.y) * (b.x - a.x) - (p.x - a.x) * (b.y - a.y)
    if (abs(cross) > 1e-9) return false

    val dot = (p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)
    if (dot < 1e-9) return false

    val squaredLength = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
    if (dot > squaredLength - 1e-9) return false

    return true
}