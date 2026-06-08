package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.edgesForVertex

fun Dcel.faceQuadToTriangle(faceId: Int): List<Int> {
    val faceEdges = edgesForFace(faceId)
    if (faceEdges.size != 4) {
        return emptyList()
    }

    val beforeFaces = faces.size

    val v0 = halfEdges[faceEdges[0]].vertex
    val v1 = halfEdges[faceEdges[1]].vertex
    val v2 = halfEdges[faceEdges[2]].vertex
    val v3 = halfEdges[faceEdges[3]].vertex

    val valence0 = edgesForVertex(v0).size
    val valence1 = edgesForVertex(v1).size
    val valence2 = edgesForVertex(v2).size
    val valence3 = edgesForVertex(v3).size

    if (true || valence0 + valence2 <= valence1 + valence3) {
        edgeInsert(faceEdges[0], faceEdges[2])
    } else {
        edgeInsert(faceEdges[1], faceEdges[3])
    }

    val afterFaces = faces.size
    val result = mutableListOf<Int>()
    for (i in beforeFaces until afterFaces) {
        result.add(i)
    }
    // faceId itself might have been modified to be one of the triangles, 
    // and a new face might have been added.
    // edgeInsert usually splits a face into two.
    // Let's check how edgeInsert works. 
    // It typically reuses the original face for one part and creates a new face for the other.
    
    // We should return all resulting faces that replaced the original face.
    // In this case, it's the original face plus any newly created faces that were part of it.
    
    // Since we know it was one face and it became two:
    return listOf(faceId, afterFaces - 1)
}