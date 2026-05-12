package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.isEdgeloop(edgeIds: List<Int>): Boolean {
    if (edgeIds.size < 2) return false

    for (i in 0 until edgeIds.size) {
        if (edgeIds[i] == -1)
            return false
        val cur = halfEdges[edgeIds[i]]
        if (cur.nextEdge == -1)
            return false

        val expectedNextVert = halfEdges[cur.nextEdge].vertex
        val nextVertex = halfEdges[edgeIds[(i + 1).mod(edgeIds.size)]].vertex
        if (expectedNextVert != nextVertex) {
            return false
        }
    }

    return true
}