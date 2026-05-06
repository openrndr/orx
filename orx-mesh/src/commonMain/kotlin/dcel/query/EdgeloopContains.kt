package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.abs

fun Dcel.faceContains(faceId: Int, point: Vector3, planarEpsilon: Double = 1e-6): Boolean {
    if (faceId == -1) return false
    val face = faces.getOrNull(faceId) ?: return false
    if (face.edge == -1) return false
    return edgeloopContains(face.edge, point, planarEpsilon)

}

fun Dcel.edgeloopContains(edgeId: Int, point: Vector3, planarEpsilon: Double = 1e-6): Boolean {
    require(edgeId >= 0 && edgeId < halfEdges.size) { "edgeId must be a valid edge index" }

    val loopIndices = edgeLoopIndices(edgeId)
    require(loopIndices.size >= 3) { "edge loop length must be at least 3" }

    val positions = loopIndices.map { vertices[halfEdges[it].vertex].position }

    // Find loop normal and check if it's planar
    var areaVector = Vector3.ZERO
    for (i in positions.indices) {
        val v0 = positions[i]
        val v1 = positions[(i + 1) % positions.size]
        areaVector += v0.cross(v1)
    }

    if (areaVector.length < 1e-12) {
        // All points are collinear, the loop has no interior
        return false
    }

    val normal = areaVector.normalized
    val d = positions[0].dot(normal)

    // Check loop planarity
    for (i in 1 until positions.size) {
        if (abs(positions[i].dot(normal) - d) > planarEpsilon) {
            error("edge loop is not planar")
        }
    }

    // Check if query point is on the plane
    if (abs(point.dot(normal) - d) > planarEpsilon) {
        return false
    }

    // Project points to 2D
    // Find an orthogonal basis for the plane
    val tangent = if (abs(normal.x) < 0.9) Vector3.UNIT_X else Vector3.UNIT_Y
    val bitangent = normal.cross(tangent).normalized
    val finalTangent = bitangent.cross(normal).normalized

    fun project(p: Vector3): Vector2 {
        return Vector2(p.dot(finalTangent), p.dot(bitangent))
    }

    val p2d = project(point)
    val positions2d = positions.map { project(it) }

    // Ray casting algorithm for point-in-polygon
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

    return inside
}
