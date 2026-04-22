package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgesForFace(face: Int): List<HalfEdge> {
    val faceObj = faces.getOrNull(face) ?: return emptyList()
    val edges = mutableListOf<HalfEdge>()
    val startEdgeIdx = faceObj.edge
    if (startEdgeIdx == -1) return emptyList()

    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        edges.add(edge)
        currentEdgeIdx = edge.nextEdge
    } while (currentEdgeIdx != startEdgeIdx && currentEdgeIdx != -1)

    return edges
}

fun Dcel.edgeLoopIndices(startEdgeIdx: Int): List<Int> {
    val edges = mutableListOf<Int>()
    if (startEdgeIdx == -1) return emptyList()
    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        edges.add(currentEdgeIdx)
        currentEdgeIdx = edge.nextEdge
    } while (currentEdgeIdx != startEdgeIdx && currentEdgeIdx != -1)

    return edges
}

