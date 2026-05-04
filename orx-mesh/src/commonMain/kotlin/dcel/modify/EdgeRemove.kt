package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgeRemove(e: HalfEdge) {
    if (e.otherEdge == -1) return
    val other = halfEdges[e.otherEdge]

    val f1Idx = e.face
    val f2Idx = other.face

    if (f1Idx == -1 || f2Idx == -1) return
    if (f1Idx == f2Idx) {
        // Special case: removing an edge that already has the same face on both sides.
        // This is exactly what the new requirement asks us to handle recursively.
        removeSingleEdge(e)
        return
    }

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
    e.face = -1
    other.face = -1
    e.vertex = -1
    other.vertex = -1

    // Face f2 is now gone
    faces[f2Idx].edge = -1

    // Invalidate the removed edges completely
    e.nextEdge = -1
    e.prevEdge = -1
    e.otherEdge = -1
    other.nextEdge = -1
    other.prevEdge = -1
    other.otherEdge = -1

    // New requirement: remove half edges that have been assigned the same face on both sides
    removeRedundantEdges(f1Idx)
}

private fun Dcel.removeSingleEdge(e: HalfEdge) {
    val otherIdx = e.otherEdge
    val other = halfEdges[otherIdx]
    val eIdx = halfEdges.indexOf(e)

    val prevEIdx = e.prevEdge
    val nextEIdx = e.nextEdge
    val prevOIdx = other.prevEdge
    val nextOIdx = other.nextEdge

    // If it's a bridge edge (both sides same face), we need to be careful.
    // The structure before removal looks like two loops connected by this edge pair.
    // e.prev -> e -> e.next
    // other.prev -> other -> other.next
    // e.vertex == other.next.vertex (if it's a standard edge)
    // Actually:
    // e starts at e.vertex, ends at other.vertex
    // other starts at other.vertex, ends at e.vertex
    
    // Case 1: Standard redundant edge (e.g. diagonal that was just added or became redundant)
    // prevE.next = nextE  (NO, if it's the same face, they are in the same loop)
    
    // Actually, if an edge has the same face on both sides, it means it's either:
    // 1. A "bridge" or "dangling" edge where removing it might split one loop into two or just shorten a loop.
    // 2. An edge that separates two parts of the same face.
    
    // Let's look at the connectivity:
    // ... -> prevE -> e -> nextE -> ...
    // ... -> prevO -> other -> nextO -> ...
    
    // To remove e and other:
    // prevE.next = nextO
    // nextO.prev = prevE
    // prevO.next = nextE
    // nextE.prev = prevO

    halfEdges[prevEIdx].nextEdge = nextOIdx
    halfEdges[nextOIdx].prevEdge = prevEIdx
    halfEdges[prevOIdx].nextEdge = nextEIdx
    halfEdges[nextEIdx].prevEdge = prevOIdx

    // Update vertex references
    if (vertices[e.vertex].edge == eIdx) {
        vertices[e.vertex].edge = nextEIdx
    }
    if (vertices[other.vertex].edge == otherIdx) {
        vertices[other.vertex].edge = nextOIdx
    }

    // Update face reference
    val fIdx = e.face
    if (fIdx != -1 && faces[fIdx].edge == eIdx) {
        faces[fIdx].edge = nextEIdx
    }
    if (fIdx != -1 && faces[fIdx].edge == otherIdx) {
        faces[fIdx].edge = nextOIdx
    }

    // Invalidate
    e.face = -1
    e.nextEdge = -1
    e.prevEdge = -1
    e.otherEdge = -1
    e.vertex = -1
    other.face = -1
    other.nextEdge = -1
    other.prevEdge = -1
    other.otherEdge = -1
    other.vertex = -1
}

private fun Dcel.removeRedundantEdges(faceIdx: Int) {
    if (faceIdx == -1) return
    
    var changed = true
    var iterations = 0
    while (changed && iterations < 100) {
        changed = false
        iterations++
        
        val redundantEdge = halfEdges.find { e ->
            e.face == faceIdx && e.otherEdge != -1 && halfEdges[e.otherEdge].face == faceIdx
        }
        
        if (redundantEdge != null) {
            removeSingleEdge(redundantEdge)
            changed = true
        }
    }
}