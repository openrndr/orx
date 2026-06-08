package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3

fun Dcel.faceNormal(faceId: Int): Vector3 {
    val faceObj = faces.getOrNull(faceId) ?: return Vector3.ZERO
    val edgeIndices = edgeLoopIndices(faceObj.edge)
    if (edgeIndices.size < 3) return Vector3.ZERO

    val positions = edgeIndices.map { vertices[halfEdges[it].vertex].position }

    var areaVector = Vector3.ZERO
    for (i in positions.indices) {
        val v0 = positions[i]
        val v1 = positions[(i + 1) % positions.size]
        areaVector += v0.cross(v1)
    }

    if (areaVector.length < 1e-12) {
        return Vector3.ZERO
    }

    val normal = areaVector.normalized
    return normal
}