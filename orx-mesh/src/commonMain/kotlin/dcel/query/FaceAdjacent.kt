package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.faceAdjacent(faceId: Int): List<Int> {
    val result = mutableSetOf<Int>()
    val edges = edgesForFace(faceId)
    for(edgeId in edges) {
        val edge = halfEdges[edgeId]
        if (edge.otherEdge != -1) {
            val other = halfEdges[edge.otherEdge]
            if (other.face != -1) {
                result.add(other.face)
            }
        }
    }
    return result.toList()
}