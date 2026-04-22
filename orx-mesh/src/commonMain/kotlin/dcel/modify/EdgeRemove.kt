package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgeRemove(e: HalfEdge) {
    if (e.otherEdge == -1) return
    val other = halfEdges[e.otherEdge]

    val f1Idx = e.face
    val f2Idx = other.face

    if (f1Idx == -1 || f2Idx == -1 || f1Idx == f2Idx) return

    val eIdx = halfEdges.indexOf(e)
    val otherIdx = e.otherEdge

    val prevEIdx = e.prevEdge
    val nextEIdx = e.nextEdge
    val prevOIdx = other.prevEdge
    val nextOIdx = other.nextEdge

    val prevE = halfEdges[prevEIdx]
    val nextE = halfEdges[nextEIdx]
    val prevO = halfEdges[prevOIdx]
    val nextO = halfEdges[nextOIdx]

    // Link the loops
    prevE.nextEdge = nextOIdx
    nextO.prevEdge = prevEIdx

    prevO.nextEdge = nextEIdx
    nextE.prevEdge = prevOIdx

    // Update face for all edges in the absorbed face (f2)
    var currIdx = nextOIdx
    // Limit to 1000 edges to avoid infinite loop if the loop isn't closed (shouldn't happen in DCEL)
    var count = 0
    while (currIdx != nextEIdx && count < 1000) {
        halfEdges[currIdx].face = f1Idx
        currIdx = halfEdges[currIdx].nextEdge
        count++
    }
    // Also update face for all edges that were in the original face (f1)
    // to ensure the entire loop is consistent, although they should already be f1Idx.
    // The previous loop only covered the edges that were added from f2.

    // Update vertex references if they pointed to the removed edges
    if (vertices[e.vertex].edge == eIdx) {
        vertices[e.vertex].edge = nextEIdx
    }
    if (vertices[other.vertex].edge == otherIdx) {
        vertices[other.vertex].edge = nextOIdx
    }

    // Update face reference
    if (faces[f1Idx].edge == eIdx) {
        faces[f1Idx].edge = nextEIdx
    }
    
    // Mark removed edges and face
    // For now we just set them to an invalid state, or we could actually remove them.
    // However, removing from middle of list breaks all indices.
    // DCEL typically needs a more robust way to handle removals.
    // If we can't remove them, we mark them.
    e.face = -1
    other.face = -1
    
    // Face f2 is now gone
    faces[f2Idx].edge = -1
    
    // Invalidate the removed edges completely
    e.nextEdge = -1
    e.prevEdge = -1
    e.otherEdge = -1
    other.nextEdge = -1
    other.prevEdge = -1
    other.otherEdge = -1
}