package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.wholeEdgeCount() : Int {
    return edgeCount() - halfEdges.count { it.vertex != -1 && it.otherEdge != -1} / 2
}

fun Dcel.edgeCount() : Int {
    return halfEdges.count { it.vertex != -1 }
}


fun Dcel.edgeBoundaryCount() : Int {
    return if (halfEdges.any { it.vertex != -1 && it.otherEdge == -1 }) 1 else 0
}