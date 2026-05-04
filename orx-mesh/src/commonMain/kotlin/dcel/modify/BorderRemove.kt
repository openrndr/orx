package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.bordersRemove(borders: List<List<Int>>) {
    // remove lists of contiguous edges from the mesh
    // removing borders joins exactly two faces
    for (border in borders) {
        if (border.isEmpty()) continue

        val firstEdgeIdx = border[0]
        val lastEdgeIdx = border.last()

        val eFirst = halfEdges[firstEdgeIdx]
        val eLast = halfEdges[lastEdgeIdx]

        if (eFirst.otherEdge == -1) continue
        val oFirst = halfEdges[eFirst.otherEdge]

        if (eLast.otherEdge == -1) continue
        val oLast = halfEdges[eLast.otherEdge]

        val f1Idx = eFirst.face
        val f2Idx = oFirst.face

        if (f1Idx == -1 || f2Idx == -1 || f1Idx == f2Idx) continue

        val prevEIdx = eFirst.prevEdge
        val nextEIdx = eLast.nextEdge

        val prevOIdx = oLast.prevEdge
        val nextOIdx = oFirst.nextEdge

        val borderSet = border.toSet()
        val otherBorderSet = border.map { halfEdges[it].otherEdge }.toSet()

        // Link the loops
        if (prevEIdx !in borderSet) {
            halfEdges[prevEIdx].nextEdge = nextOIdx
            halfEdges[nextOIdx].prevEdge = prevEIdx
        }

        if (prevOIdx !in otherBorderSet) {
            halfEdges[prevOIdx].nextEdge = nextEIdx
            halfEdges[nextEIdx].prevEdge = prevOIdx
        }

        // Update face for all edges in the absorbed face (f2)
        // We need to find a starting edge that is NOT in the border being removed.
        var startIdx = -1
        if (nextEIdx !in borderSet) {
            startIdx = nextEIdx
        } else if (nextOIdx !in otherBorderSet) {
            startIdx = nextOIdx
        }

        if (startIdx != -1) {
            var currIdx = startIdx
            var count = 0
            do {
                halfEdges[currIdx].face = f1Idx
                currIdx = halfEdges[currIdx].nextEdge
                count++
            } while (currIdx != startIdx && count < 10000)

            if (faces[f1Idx].edge in borderSet || faces[f1Idx].edge in otherBorderSet) {
                faces[f1Idx].edge = startIdx
            }
        } else {
            // All edges were in the border? This means the entire face was the border.
            faces[f1Idx].edge = -1
        }

        // Update vertex references
        val allRemoved = borderSet + otherBorderSet
        for (eIdx in border) {
            val e = halfEdges[eIdx]
            val oIdx = e.otherEdge
            val o = halfEdges[oIdx]

            if (vertices[e.vertex].edge in allRemoved) {
                var found = -1
                var curr = eIdx
                var countV = 0
                // Rotate around e.vertex
                do {
                    val other = halfEdges[curr].otherEdge
                    if (other == -1) break
                    curr = halfEdges[other].nextEdge
                    if (curr !in allRemoved) {
                        found = curr
                        break
                    }
                    countV++
                } while (curr != eIdx && countV < 1000)
                vertices[e.vertex].edge = found
            }
            if (vertices[o.vertex].edge in allRemoved) {
                var found = -1
                var curr = oIdx
                var countV = 0
                // Rotate around o.vertex
                do {
                    val other = halfEdges[curr].otherEdge
                    if (other == -1) break
                    curr = halfEdges[other].nextEdge
                    if (curr !in allRemoved) {
                        found = curr
                        break
                    }
                    countV++
                } while (curr != oIdx && countV < 1000)
                vertices[o.vertex].edge = found
            }
        }

        // Mark removed edges and face
        for (eIdx in border) {
            val e = halfEdges[eIdx]
            val oIdx = e.otherEdge
            val o = halfEdges[oIdx]

            e.face = -1
            e.nextEdge = -1
            e.prevEdge = -1
            e.otherEdge = -1
            e.vertex = -1

            o.face = -1
            o.nextEdge = -1
            o.prevEdge = -1
            o.otherEdge = -1
            o.vertex = -1
        }

        // Face f2 is now gone
        faces[f2Idx].edge = -1
    }
}