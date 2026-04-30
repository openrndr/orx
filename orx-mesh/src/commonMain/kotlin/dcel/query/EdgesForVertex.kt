package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

/**
Lists all half edges that start at the given vertex
 */
fun Dcel.edgesForVertex(vertexId: Int): List<Int> {
    if (vertexId < 0 || vertexId >= vertices.size) return emptyList()
    val result = mutableListOf<Int>()
    val startEdge = vertices[vertexId].edge
    if (startEdge != -1) {
        var currentEdge = startEdge
        // Rotate in one direction
        while (currentEdge != -1) {
            result.add(currentEdge)
            val other = halfEdges[currentEdge].otherEdge
            if (other == -1) break
            currentEdge = halfEdges[other].nextEdge
            if (currentEdge == startEdge) return result
        }

        // If we broke out because of a boundary, rotate in the other direction from start
        currentEdge = startEdge
        while (true) {
            val prev = halfEdges[currentEdge].prevEdge
            currentEdge = halfEdges[prev].otherEdge
            if (currentEdge == -1 || currentEdge == startEdge) break
            result.add(currentEdge)
        }
    }
    return result
}

/**
 * Lists all half edges that end at the given vertex
 * @param vertexId The index of the vertex to query
 * @return A list of half edge indices that end at the specified vertex
 */
fun Dcel.edgesEndingAtVertex(vertexId: Int): List<Int> {
    if (vertexId < 0 || vertexId >= vertices.size) return emptyList()
    val result = mutableListOf<Int>()
    val startEdge = vertices[vertexId].edge
    if (startEdge != -1) {
        var currentEdge = startEdge
        // Rotate in one direction
        while (currentEdge != -1) {
            val prev = halfEdges[currentEdge].prevEdge
            result.add(prev)
            val other = halfEdges[currentEdge].otherEdge
            if (other == -1) break
            currentEdge = halfEdges[other].nextEdge
            if (currentEdge == startEdge) return result
        }

        // If we broke out because of a boundary, rotate in the other direction from start
        currentEdge = startEdge
        while (true) {
            val prevEdge = halfEdges[currentEdge].prevEdge
            val other = halfEdges[prevEdge].otherEdge
            if (other == -1 || other == startEdge) break
            currentEdge = other
            val currentPrev = halfEdges[currentEdge].prevEdge
            result.add(currentPrev)
        }
    }
    return result
}