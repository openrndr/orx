package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.math.Vector3

fun Dcel.edgeSetOffset(edgeIds: Set<Int>, offset: Double): Set<Int> {
    if (edgeIds.isEmpty()) return emptySet()

    // 1. Group edgeIds into contiguous chains.
    // Each chain is a list of edge indices [e1, e2, ..., en] where e_{i+1} is halfEdges[e_i].nextEdge.
    val remainingEdges = edgeIds.toMutableSet()
    val chains = mutableListOf<List<Int>>()

    while (remainingEdges.isNotEmpty()) {
        val startEdgeIdx = remainingEdges.first()
        remainingEdges.remove(startEdgeIdx)

        val chain = mutableListOf(startEdgeIdx)

        // Forward
        var curr = startEdgeIdx
        while (true) {
            val next = halfEdges[curr].nextEdge
            if (next != -1 && next in remainingEdges) {
                chain.add(next)
                remainingEdges.remove(next)
                curr = next
                if (curr == startEdgeIdx) break // Should not happen with remainingEdges check, but for safety
            } else {
                break
            }
        }

        // Backward
        curr = startEdgeIdx
        while (true) {
            val prev = halfEdges[curr].prevEdge
            if (prev != -1 && prev in remainingEdges) {
                chain.add(0, prev)
                remainingEdges.remove(prev)
                curr = prev
            } else {
                break
            }
        }

        // Check if it's a closed loop
        val first = chain.first()
        val last = chain.last()
        val closed = halfEdges[last].nextEdge == first

        chains.add(chain)
    }

    val newFaceIds = mutableSetOf<Int>()

    for (chain in chains) {
        val isClosed = halfEdges[chain.last()].nextEdge == chain.first()

        // 2. Compute offset directions for each vertex in the chain.
        // For each edge in the chain, we want its "outer" normal.
        // Assuming the mesh is in XY plane (as common in ORX 2D mesh work), or generally 3D.
        // Since it's a boundary edge (otherEdge == -1), we assume the face it belongs to defines its orientation.
        // Let's assume we use a "face normal" to determine the perpendicular direction.
        // If it's 2D, we can just use (v1-v0).cross(Z).

        fun getEdgeNormal(eIdx: Int): Vector3 {
            val e = halfEdges[eIdx]
            val v0 = vertices[e.vertex].position
            val v1 = vertices[halfEdges[e.nextEdge].vertex].position
            val edgeDir = (v1 - v0).normalized

            // Try to find a face normal to determine "up"
            var up = Vector3.UNIT_Z
            if (e.face != -1) {
                val f = faces[e.face]
                // Simple normal calculation
                val e0 = halfEdges[f.edge]
                val vA = vertices[e0.vertex].position
                val vB = vertices[halfEdges[e0.nextEdge].vertex].position
                val vC = vertices[halfEdges[halfEdges[e0.nextEdge].nextEdge].vertex].position
                val fn = (vB - vA).cross(vC - vB).normalized
                if (fn.length > 0.1) {
                    up = fn
                }
            }
            // Boundary edge: edge.face is the face it IS part of.
            // If the face is on the "left", then (v1-v0) cross normal points "right" (outside).
            return up.cross(edgeDir).normalized
        }

        val edgeNormals = chain.map { getEdgeNormal(it) }
        val vertexOffsets = mutableListOf<Vector3>()

        if (isClosed) {
            for (i in chain.indices) {
                val nPrev = edgeNormals[(i + chain.size - 1) % chain.size]
                val nCurr = edgeNormals[i]
                val vOffset = (nPrev + nCurr).normalized
                // miter length factor: 1 / cos(half_angle)
                val dot = nPrev.dot(nCurr).coerceIn(-1.0, 1.0)
                val cosHalfAngle = kotlin.math.sqrt((1.0 + dot) / 2.0)
                val length = if (cosHalfAngle > 0.1) 1.0 / cosHalfAngle else 1.0
                vertexOffsets.add(vOffset * length * offset)
            }
        } else {
            // First vertex
            vertexOffsets.add(edgeNormals.first() * offset)
            // Middle vertices
            for (i in 1 until chain.size) {
                val nPrev = edgeNormals[i - 1]
                val nCurr = edgeNormals[i]
                val vOffset = (nPrev + nCurr).normalized
                val dot = nPrev.dot(nCurr).coerceIn(-1.0, 1.0)
                val cosHalfAngle = kotlin.math.sqrt((1.0 + dot) / 2.0)
                val length = if (cosHalfAngle > 0.1) 1.0 / cosHalfAngle else 1.0
                vertexOffsets.add(vOffset * length * offset)
            }
            // Last vertex
            vertexOffsets.add(edgeNormals.last() * offset)
        }

        // 3. Create new vertices and faces
        // Original vertices in chain: v0, v1, ..., vn, (v0 if closed)
        // New vertices: nv0, nv1, ..., nvn
        val chainVertices = chain.map { halfEdges[it].vertex }
        if (!isClosed) {
            chainVertices.toMutableList().add(halfEdges[halfEdges[chain.last()].nextEdge].vertex)
        }
        // Wait, the number of vertices is chain.size + 1 if not closed, and chain.size if closed.
        val originalVertexIndices = mutableListOf<Int>()
        for (eIdx in chain) {
            originalVertexIndices.add(halfEdges[eIdx].vertex)
        }
        if (!isClosed) {
            originalVertexIndices.add(halfEdges[halfEdges[chain.last()].nextEdge].vertex)
        }

        val newVertexIndices = originalVertexIndices.indices.map { i ->
            val vIdx = originalVertexIndices[i]
            val pos = vertices[vIdx].position + vertexOffsets[i]
            val nvIdx = vertices.size
            vertices.add(Vertex(pos, -1))
            nvIdx
        }

        // For each edge in chain, create a quad face
        for (i in chain.indices) {
            val eIdx = chain[i]
            val e = halfEdges[eIdx]

            val v0 = originalVertexIndices[i]
            val v1 = originalVertexIndices[(i + 1) % originalVertexIndices.size]
            val nv0 = newVertexIndices[i]
            val nv1 = newVertexIndices[(i + 1) % newVertexIndices.size]

            val fIdx = faces.size
            faces.add(Face(-1))
            newFaceIds.add(fIdx)

            val e0Idx = halfEdges.size
            val e1Idx = halfEdges.size + 1
            val e2Idx = halfEdges.size + 2
            val e3Idx = halfEdges.size + 3

            // e0: nv0 -> nv1 (outer edge)
            // e1: nv1 -> v1
            // e2: v1 -> v0 (the original edge will be e2.otherEdge, but e2 is part of new face)
            // e3: v0 -> nv0

            val e0 = HalfEdge(fIdx, nv0, e1Idx, e3Idx, -1, halfEdges[eIdx].attributes.copyOf())
            val e1 = HalfEdge(fIdx, nv1, e2Idx, e0Idx, -1, halfEdges[eIdx].attributes.copyOf()) // TODO: attribute interpolation?
            val e2 = HalfEdge(fIdx, v1, e3Idx, e1Idx, eIdx, halfEdges[eIdx].attributes.copyOf())
            val e3 = HalfEdge(fIdx, v0, e0Idx, e2Idx, -1, halfEdges[eIdx].attributes.copyOf())

            halfEdges.add(e0)
            halfEdges.add(e1)
            halfEdges.add(e2)
            halfEdges.add(e3)

            faces[fIdx].edge = e0Idx

            // Link original edge to e2
            halfEdges[eIdx].otherEdge = e2Idx

            // Update vertex.edge if it was -1
            if (vertices[nv0].edge == -1) vertices[nv0].edge = e0Idx
            if (vertices[nv1].edge == -1) vertices[nv1].edge = e1Idx
        }

        // Link new edges between adjacent faces in the chain
        val facesInChain = newFaceIds.toList().takeLast(chain.size)
        for (i in chain.indices) {
            val currFaceIdx = facesInChain[i]
            val nextFaceIdx = if (i < chain.size - 1) {
                facesInChain[i + 1]
            } else if (isClosed) {
                facesInChain[0]
            } else {
                -1
            }

            if (nextFaceIdx != -1) {
                val e1OfCurr = faces[currFaceIdx].edge + 1
                val e3OfNext = faces[nextFaceIdx].edge + 3
                halfEdges[e1OfCurr].otherEdge = e3OfNext
                halfEdges[e3OfNext].otherEdge = e1OfCurr
            }
        }
    }

    return newFaceIds
}