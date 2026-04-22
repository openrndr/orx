package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.facesForVertex(vertex: Int): List<Face> {
    val vertexObj = vertices.getOrNull(vertex) ?: return emptyList()
    val startEdgeIdx = vertexObj.edge
    if (startEdgeIdx == -1) return emptyList()

    val result = mutableSetOf<Int>()
    
    // Direction 1: counter-clockwise (if vertexObj.edge points CCW)
    // Actually, following the pointers:
    // Starting from edge starting at V, its prevEdge ends at V.
    // The otherEdge of that prevEdge starts at V and belongs to the adjacent face.
    
    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        if (edge.face != -1) {
            result.add(edge.face)
        }
        
        val prevEdgeIdx = edge.prevEdge
        if (prevEdgeIdx == -1) break
        
        val prevEdge = halfEdges[prevEdgeIdx]
        currentEdgeIdx = prevEdge.otherEdge
        
    } while (currentEdgeIdx != -1 && currentEdgeIdx != startEdgeIdx)

    // If we hit a boundary (currentEdgeIdx == -1), we need to go the other way from startEdgeIdx
    if (currentEdgeIdx == -1) {
        currentEdgeIdx = startEdgeIdx
        while (true) {
            val edge = halfEdges[currentEdgeIdx]
            val otherEdgeIdx = edge.otherEdge
            if (otherEdgeIdx == -1) break
            
            val otherEdge = halfEdges[otherEdgeIdx]
            currentEdgeIdx = otherEdge.nextEdge
            if (currentEdgeIdx == -1 || currentEdgeIdx == startEdgeIdx) break
            
            val nextEdge = halfEdges[currentEdgeIdx]
            if (nextEdge.face != -1) {
                result.add(nextEdge.face)
            }
        }
    }

    return result.map { faces[it] }
}