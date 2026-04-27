package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector3

fun Dcel.convexFaceCenter(faceId: Int): Vector3 {
    val faceObj = faces.getOrNull(faceId) ?: return Vector3.ZERO
    val startEdgeIdx = faceObj.edge
    if (startEdgeIdx == -1) return Vector3.ZERO

    var sum = Vector3.ZERO
    var count = 0
    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        val vertex = vertices[edge.vertex]
        sum += vertex.position
        count++
        currentEdgeIdx = edge.nextEdge
    } while (currentEdgeIdx != startEdgeIdx && currentEdgeIdx != -1)

    return if (count > 0) sum / count.toDouble() else Vector3.ZERO
}