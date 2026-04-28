package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.extra.mesh.dcel.query.edgePoint
import org.openrndr.extra.mesh.dcel.query.edgesForVertex

fun Dcel.vertexChamfer(vertexId: Int, radius: Double): Int {
    val outgoingEdges = edgesForVertex(vertexId)
    if (outgoingEdges.isEmpty()) return -1

    // 1. Create new vertices along each outgoing edge
    val edgeToNewVertex = mutableMapOf<Int, Int>()

    for (eIdx in outgoingEdges) {
        val edge = halfEdges[eIdx]
        val v0 = vertices[edge.vertex].position
        val nextEdgeIdx = edge.nextEdge
        val v1 = vertices[halfEdges[nextEdgeIdx].vertex].position
        val dist = (v1 - v0).length
        val t = if (dist > 0.0) (radius / dist).coerceIn(0.0, 1.0) else 0.0

        val point = edgePoint(eIdx, t)
        val newVertexIdx = vertices.size
        vertices.add(Vertex(point.position, eIdx))

        val colorIdx = point.color?.let { colors.add(it); colors.size - 1 } ?: -1
        val texCoordIdx = point.textureCoordinate?.let { textureCoordinates.add(it); textureCoordinates.size - 1 } ?: -1
        val normalIdx = point.normal?.let { normals.add(it); normals.size - 1 } ?: -1
        val tangentIdx = point.tangent?.let { tangents.add(it); tangents.size - 1 } ?: -1
        val bitangentIdx = point.bitangent?.let { bitangents.add(it); bitangents.size - 1 } ?: -1

        val newAttrs = intArrayOf(colorIdx, texCoordIdx, normalIdx, tangentIdx, bitangentIdx)

        edgeToNewVertex[eIdx] = newVertexIdx
        halfEdges[eIdx].attributes = newAttrs
        halfEdges[eIdx].vertex = newVertexIdx
    }

    // 2. Create the new chamfer face
    val chamferFaceIdx = faces.size
    faces.add(Face(-1))

    // 3. Insert bridge edges in original faces and prepare chamfer face edges
    val outgoingToBridge = mutableMapOf<Int, Int>()

    for (eIdx in outgoingEdges) {
        val faceIdx = halfEdges[eIdx].face
        if (faceIdx == -1) continue

        val incomingIdx = halfEdges[eIdx].prevEdge
        if (incomingIdx == -1) continue

        // The outgoing edge that starts at vertexId and is part of the "previous" face (CW)
        // is halfEdges[incomingIdx].otherEdge
        val ePrevIdx = halfEdges[incomingIdx].otherEdge

        if (ePrevIdx == -1) continue

        val vBridge = edgeToNewVertex[ePrevIdx]!!

        val bridgeIdx = halfEdges.size
        outgoingToBridge[eIdx] = bridgeIdx

        val bridge = HalfEdge(
            face = faceIdx,
            vertex = vBridge,
            nextEdge = eIdx,
            prevEdge = incomingIdx,
            otherEdge = -1,
            attributes = halfEdges[ePrevIdx].attributes.copyOf()
        )
        halfEdges.add(bridge)

        halfEdges[incomingIdx].nextEdge = bridgeIdx
        halfEdges[eIdx].prevEdge = bridgeIdx
    }

    // 4. Create edges for the chamfer face
    val chamferEdges = mutableListOf<Int>()
    for (eIdx in outgoingEdges) {
        val bridgeIdx = outgoingToBridge[eIdx] ?: continue

        val chamferEdgeIdx = halfEdges.size
        chamferEdges.add(chamferEdgeIdx)

        val chamferEdge = HalfEdge(
            face = chamferFaceIdx,
            vertex = edgeToNewVertex[eIdx]!!,
            nextEdge = -1,
            prevEdge = -1,
            otherEdge = bridgeIdx,
            attributes = halfEdges[eIdx].attributes.copyOf()
        )
        halfEdges.add(chamferEdge)
        halfEdges[bridgeIdx].otherEdge = chamferEdgeIdx
    }

    // 5. Link chamfer edges in CCW order
    for (i in chamferEdges.indices) {
        val curr = chamferEdges[i]
        val next = chamferEdges[(i + 1) % chamferEdges.size]
        val prev = chamferEdges[(i + chamferEdges.size - 1) % chamferEdges.size]
        halfEdges[curr].nextEdge = next
        halfEdges[curr].prevEdge = prev
    }

    if (chamferEdges.isNotEmpty()) {
        faces[chamferFaceIdx].edge = chamferEdges[0]
    }

    vertices[vertexId].edge = -1
    return chamferFaceIdx
}