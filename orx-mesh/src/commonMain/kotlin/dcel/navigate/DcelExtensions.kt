package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.VertexList

fun Dcel.canonicalEdges(): EdgeList {
    val visited = mutableSetOf<Int>()
    val result = mutableListOf<Int>()

    for (i in halfEdges.indices) {
        if (i in visited) continue

        val edge = halfEdges[i]
        val otherEdgeIndex = edge.otherEdge

        visited.add(i)

        if (otherEdgeIndex == -1) {
            result.add(i)
        } else {
            visited.add(otherEdgeIndex)
            result.add(minOf(i, otherEdgeIndex))
        }
    }

    return EdgeList(result)
}

fun Dcel.allEdges(): EdgeList {
    val result = mutableListOf<Int>()
    for (i in halfEdges.indices) {
        val edge = halfEdges[i]
        if (edge.face != -1) {
            result.add(i)
        }
    }
    return EdgeList(result)
}

fun Dcel.allVertices(): VertexList {
    val result = mutableListOf<Int>()
    for (i in vertices.indices) {
        val vertex = vertices[i]
        if (vertex.edge != -1) {
            result.add(i)
        }
    }
    return VertexList(result)
}

fun Dcel.allFaces(): FaceList {
    val result = mutableListOf<Int>()
    for (i in faces.indices) {
        val face = faces[i]
        if (face.edge != -1) {
            result.add(i)
        }
    }
    return FaceList(result)
}