package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.bordersForEdge(edgeIdx: Int): List<List<Int>> {
    val e0 = halfEdges.getOrNull(edgeIdx) ?: return emptyList()
    val f1Idx = e0.face
    val e0OtherIdx = e0.otherEdge
    val e0Other = halfEdges.getOrNull(e0OtherIdx) ?: return emptyList()
    val f2Idx = e0Other.face

    if (f1Idx == -1 || f2Idx == -1 || f1Idx == f2Idx) return emptyList()

    val f1Edges = edgeLoopIndices(faces[f1Idx].edge)
    
    val sharedEdges = f1Edges.filter {
        val otherIdx = halfEdges[it].otherEdge
        val other = halfEdges.getOrNull(otherIdx)
        other?.face == f2Idx
    }.toSet()

    if (sharedEdges.isEmpty()) return emptyList()

    val borders = mutableListOf<List<Int>>()
    val visited = mutableSetOf<Int>()

    for (startEdge in sharedEdges) {
        if (startEdge in visited) continue

        val currentBorder = mutableListOf<Int>()
        currentBorder.add(startEdge)
        visited.add(startEdge)

        // search forward
        var curr = startEdge
        while (true) {
            val next = halfEdges[curr].nextEdge
            if (next in sharedEdges && next !in visited) {
                currentBorder.add(next)
                visited.add(next)
                curr = next
            } else {
                break
            }
        }

        // search backward
        curr = startEdge
        while (true) {
            val prev = halfEdges[curr].prevEdge
            if (prev in sharedEdges && prev !in visited) {
                currentBorder.add(0, prev)
                visited.add(prev)
                curr = prev
            } else {
                break
            }
        }

        // if the border is circular, the start and end should connect
        val last = currentBorder.last()
        val first = currentBorder.first()
        if (halfEdges[last].nextEdge == first && currentBorder.size > 1) {
            // it's a closed loop, which is already handled by forward search 
            // visiting all edges before returning to startEdge.
        }

        borders.add(currentBorder)
    }

    return borders
}