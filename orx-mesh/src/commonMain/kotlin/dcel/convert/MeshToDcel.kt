package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.IMeshData
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex

fun IMeshData.toDcel(): Dcel {
    val dcel = Dcel()
    dcel.vertices.addAll(vertexData.positions.map { Vertex(it, -1) })
    dcel.colors.addAll(vertexData.colors)
    dcel.textureCoordinates.addAll(vertexData.textureCoords)
    dcel.normals.addAll(vertexData.normals)
    dcel.tangents.addAll(vertexData.tangents)
    dcel.bitangents.addAll(vertexData.bitangents)

    val edgeMap = mutableMapOf<Pair<Int, Int>, Int>()

    for ((faceIdx, polygon) in polygons.withIndex()) {
        val face = Face(-1)
        val faceEdges = mutableListOf<Int>()
        val n = polygon.positions.size

        for (i in 0 until n) {
            val v0 = polygon.positions[i]
            val v1 = polygon.positions[(i + 1) % n]

            val attributes = IntArray(5)
            attributes[0] = if (polygon.colors.size > i) polygon.colors[i] else -1
            attributes[1] = if (polygon.textureCoords.size > i) polygon.textureCoords[i] else -1
            attributes[2] = if (polygon.normals.size > i) polygon.normals[i] else -1
            attributes[3] = if (polygon.tangents.size > i) polygon.tangents[i] else -1
            attributes[4] = if (polygon.bitangents.size > i) polygon.bitangents[i] else -1

            val edgeIdx = dcel.halfEdges.size
            val edge = HalfEdge(faceIdx, v0, -1, -1, -1, attributes)
            dcel.halfEdges.add(edge)
            faceEdges.add(edgeIdx)

            edgeMap[Pair(v0, v1)] = edgeIdx

            if (dcel.vertices[v0].edge == -1) {
                dcel.vertices[v0].edge = edgeIdx
            }
        }

        face.edge = faceEdges[0]
        dcel.faces.add(face)

        for (i in 0 until n) {
            val curr = faceEdges[i]
            val next = faceEdges[(i + 1) % n]
            val prev = faceEdges[(i + n - 1) % n]
            dcel.halfEdges[curr].nextEdge = next
            dcel.halfEdges[curr].prevEdge = prev
        }
    }

    for ((edgeIdx, edge) in dcel.halfEdges.withIndex()) {
        val v0 = edge.vertex
        val v1 = dcel.halfEdges[edge.nextEdge].vertex
        val otherIdx = edgeMap[Pair(v1, v0)]
        if (otherIdx != null) {
            edge.otherEdge = otherIdx
            dcel.halfEdges[otherIdx].otherEdge = edgeIdx
        }
    }

    return dcel
}