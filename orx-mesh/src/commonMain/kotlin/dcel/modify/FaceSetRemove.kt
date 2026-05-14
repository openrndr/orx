package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.edgesForVertex

fun Dcel.faceSetRemove(faceIds: Set<Int>) {
    val vertexIdsToCheck = mutableSetOf<Int>()

    val edgesToInvalidate = mutableSetOf<Int>()
    for (faceId in faceIds) {
        val faceObj = faces.getOrNull(faceId) ?: continue
        if (faceObj.edge == -1) continue

        val allLoops = listOf(faceObj.edge) + faceObj.holeEdges.toList()
        for (startEdgeIdx in allLoops) {
            val loopEdges = edgeLoopIndices(startEdgeIdx)
            for (edgeIdx in loopEdges) {
                val edge = halfEdges[edgeIdx]
                edge.face = -1
                val other = edge.otherEdge
                if (other != -1) {
                    halfEdges[other].otherEdge = -1
                }
                edge.otherEdge = -1
                vertexIdsToCheck.add(edge.vertex)
                edgesToInvalidate.add(edgeIdx)
            }
        }
        faceObj.edge = -1
        faceObj.holeEdges = IntArray(0)
    }

    for (vertexId in vertexIdsToCheck) {
        val vertex = vertices[vertexId]
        if (vertex.edge == -1) continue

        if (edgesToInvalidate.contains(vertex.edge)) {
            // Find an edge starting from this vertex that still has a face
            var found = false
            for (i in 0 until halfEdges.size) {
                if (halfEdges[i].vertex == vertexId && halfEdges[i].face != -1) {
                    vertex.edge = i
                    found = true
                    break
                }
            }
            if (!found) {
                vertex.edge = -1
            }
        }
    }
}