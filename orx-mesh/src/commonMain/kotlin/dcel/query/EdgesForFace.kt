package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge


fun Dcel.edgeForFaces(leftFace: Int, rightFace: Int): Int {
    if (leftFace == -1 || rightFace == -1) return -1
    val leftFaceObj = faces.getOrNull(leftFace) ?: return -1
    if (leftFaceObj.edge == -1) return -1
    val rightFaceObj = faces.getOrNull(rightFace) ?: return -1
    if (rightFaceObj.edge == -1) return -1

    return edgesForFace(leftFace).find {
        val other = halfEdges.getOrNull(halfEdges[it].otherEdge)
        other?.face == rightFace
    } ?: -1

}

fun Dcel.edgesForFaces(faces: Set<Int>): Set<Int> {
    val result = mutableSetOf<Int>()
    for (face in faces) {
        val edges = edgesForFace(face)
        for (edgeIdx in edges) {
            val otherEdgeIdx = halfEdges[edgeIdx].otherEdge
            if (otherEdgeIdx != -1 && result.contains(otherEdgeIdx)) {
                // edge already added by its other half-edge
            } else {
                result.add(edgeIdx)
            }
        }
    }
    return result
}

fun Dcel.edgesForFace(face: Int): List<Int> {
    val faceObj = faces.getOrNull(face) ?: return emptyList()
    val edges = mutableListOf<Int>()
    val startEdgeIdx = faceObj.edge
    if (startEdgeIdx == -1) return emptyList()

    var currentEdgeIdx = startEdgeIdx
    do {
        val edge = halfEdges[currentEdgeIdx]
        edges.add(currentEdgeIdx)
        currentEdgeIdx = edge.nextEdge
    } while (currentEdgeIdx != startEdgeIdx && currentEdgeIdx != -1)

    return edges
}

fun Dcel.edgeObjectsForFace(face: Int): List<HalfEdge> {
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

