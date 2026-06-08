package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

/**
 * find 1-ring neighbors for the vertex identified by [vertexId], the set of vertices directly connected to it by an edge.
 * @return list of vertex ids of the 1-ring neighbors
 */
fun Dcel.vertexNeighbors(vertexId: Int): List<Int> {
    val edges = edgesForVertex(vertexId)
    val result = mutableSetOf<Int>()
    for (edgeIdx in edges) {
        val edge = halfEdges[edgeIdx]
        val nextEdge = halfEdges[edge.nextEdge]
        result.add(nextEdge.vertex)

        val prevEdge = halfEdges[edge.prevEdge]
        result.add(prevEdge.vertex)
    }
    result.remove(vertexId)
    return result.toList()
}