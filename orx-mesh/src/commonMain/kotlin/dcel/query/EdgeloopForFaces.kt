package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.navigate.isBoundary

fun Dcel.edgeloopForFaces(faceIds: List<Int>): List<Int> {
    val faceSet = faceIds.toSet()
    if (faceSet.isEmpty()) return emptyList()

    val boundaryEdges = mutableSetOf<Int>()
    for (faceId in faceSet) {
        val edges = edgesForFace(faceId)
        for (edgeId in edges) {
            val edge = halfEdges[edgeId]
            if (edge.isBoundary) {
                boundaryEdges.add(edgeId)
            } else {
                val otherEdge = halfEdges[edge.otherEdge]
                if (otherEdge.face !in faceSet) {
                    boundaryEdges.add(edgeId)
                }
            }
        }
    }

    if (boundaryEdges.isEmpty()) return emptyList()

    val result = mutableListOf<Int>()
    val startEdgeId = boundaryEdges.first()
    var currentEdgeId = startEdgeId

    do {
        result.add(currentEdgeId)
        boundaryEdges.remove(currentEdgeId)

        // The next edge in the loop starts at currentEdge's end vertex.
        // We can find it by looking at currentEdge.nextEdge and its orbits.
        var candidateEdgeId = halfEdges[currentEdgeId].nextEdge
        while (candidateEdgeId != -1) {
            if (candidateEdgeId in boundaryEdges) {
                break
            }
            val otherEdgeId = halfEdges[candidateEdgeId].otherEdge
            if (otherEdgeId == -1) {
                // This should not happen if faceIds is a connected component and we are on boundary
                candidateEdgeId = -1
                break
            }
            candidateEdgeId = halfEdges[otherEdgeId].nextEdge
        }
        currentEdgeId = candidateEdgeId
    } while (currentEdgeId != -1 && currentEdgeId != startEdgeId)

    return result
}