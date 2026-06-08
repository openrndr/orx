package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3

fun Dcel.faceBasis(faceId: Int): Matrix44 {
    val faceObj = faces.getOrNull(faceId) ?: return Matrix44.IDENTITY
    val edgeIndices = edgeLoopIndices(faceObj.edge)
    if (edgeIndices.size < 3) return Matrix44.IDENTITY

    val positions = edgeIndices.map { vertices[halfEdges[it].vertex].position }

    var areaVector = Vector3.ZERO
    var centroid = Vector3.ZERO
    for (i in positions.indices) {
        val v0 = positions[i]
        val v1 = positions[(i + 1) % positions.size]
        areaVector += v0.cross(v1)
        centroid += v0
    }
    centroid /= positions.size.toDouble()

    if (areaVector.length < 1e-12) {
        return Matrix44.IDENTITY
    }

    val normal = areaVector.normalized
    val tangent = if (kotlin.math.abs(normal.z) < 0.9) Vector3.UNIT_Z else Vector3.UNIT_Y
    val bitangent = normal.cross(tangent).normalized
    val finalTangent = bitangent.cross(normal).normalized

    return Matrix44.fromColumnVectors(
        finalTangent.xyz0,
        bitangent.xyz0,
        normal.xyz0,
        centroid.xyz1
    )
}