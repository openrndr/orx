package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.verticesForFace(faceId: Int): List<Int> {
    val faceObj = faces.getOrNull(faceId) ?: return emptyList()
    val edges = mutableListOf<Int>()
    val startEdgeIdx = faceObj.edge
    if (startEdgeIdx == -1) return emptyList()

    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        edges.add(edge.vertex)
        currentEdgeIdx = edge.nextEdge
    } while (currentEdgeIdx != startEdgeIdx && currentEdgeIdx != -1)
    return edges

}