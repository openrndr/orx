package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel

import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgeInsert(start: Int, end: Int) {
    // insert a new halfEdge
    // the start edge and the end edge are part of the same face
    // the start edge and the end edge are not each other's neighbors
    // inserting the edge will split the face

    val eStart = halfEdges[start]
    val eEnd = halfEdges[end]

    if (eStart.face != eEnd.face || eStart.face == -1) {
        return
    }

    if (eStart.nextEdge == end || eEnd.nextEdge == start) {
        // They are neighbors, cannot split with a new edge (or it would be a degenerate edge)
        return
    }

    val faceIdx = eStart.face
    val newFaceIdx = faces.size

    val eStartPrev = halfEdges[eStart.prevEdge]
    val eEndPrev = halfEdges[eEnd.prevEdge]

    // New half edges
    val he0Idx = halfEdges.size
    val he1Idx = halfEdges.size + 1

    // he0 goes from start.vertex to end.vertex
    // it will be part of the original faceIdx
    val he0 = HalfEdge(
        face = faceIdx,
        vertex = eStart.vertex,
        nextEdge = end,
        prevEdge = eStart.prevEdge,
        otherEdge = he1Idx,
        attributes = eStart.attributes.copyOf()
    )

    // he1 goes from end.vertex to start.vertex
    // it will be part of a new face
    val he1 = HalfEdge(
        face = newFaceIdx,
        vertex = eEnd.vertex,
        nextEdge = start,
        prevEdge = eEnd.prevEdge,
        otherEdge = he0Idx,
        attributes = eEnd.attributes.copyOf()
    )

    halfEdges.add(he0)
    halfEdges.add(he1)

    // Re-link existing edges
    eStartPrev.nextEdge = he0Idx
    eEndPrev.nextEdge = he1Idx
    eStart.prevEdge = he1Idx
    eEnd.prevEdge = he0Idx

    // Create new face
    faces.add(Face(he1Idx))
    
    // Update faceIdx's edge if it was one of the reassigned ones
    faces[faceIdx].edge = he0Idx

    // Update face property for all edges that now belong to newFaceIdx
    var curr = start
    var count = 0
    while (curr != he1Idx && count < 1000) {
        halfEdges[curr].face = newFaceIdx
        curr = halfEdges[curr].nextEdge
        count++
    }
}