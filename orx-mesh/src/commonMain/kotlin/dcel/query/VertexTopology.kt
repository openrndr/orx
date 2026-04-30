package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel


fun Dcel.isVertexOnBoundary(vertexId: Int, edges: List<Int> = edgesForVertex(vertexId)): Boolean {
    return edges.any { halfEdges[it].otherEdge == -1 }
}

fun Dcel.isVertexABoundaryCorner(vertexId: Int, edges: List<Int> = edgesForVertex(vertexId)): Boolean {
    return edges.size == 1 && isVertexOnBoundary(vertexId, edges)
}

