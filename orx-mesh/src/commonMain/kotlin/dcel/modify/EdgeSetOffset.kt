package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.extra.shapes.polygon.Polygon3D
import org.openrndr.math.Vector3

fun Dcel.edgeSetOffset(edgeIds: List<Int>, offset: Double, useJoins: Boolean = false): List<Int> {
    return edgeSetOffset(edgeIds.toSet(), offset, useJoins).toList()
}

fun Dcel.edgeSetOffset(edgeIds: Set<Int>, offset: Double, useJoins: Boolean = false,
                       checkFace: (Polygon3D) -> Boolean = { true }): Set<Int> {
    if (edgeIds.isEmpty()) return FaceSet(emptySet())

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
            val currEndVertex = halfEdges[halfEdges[curr].nextEdge].vertex
            val next = remainingEdges.find { halfEdges[it].vertex == currEndVertex }
            if (next != null) {
                chain.add(next)
                remainingEdges.remove(next)
                curr = next
                if (curr == startEdgeIdx) break
            } else {
                break
            }
        }

        // Backward
        curr = startEdgeIdx
        while (true) {
            val currStartVertex = halfEdges[curr].vertex
            val prev = remainingEdges.find { halfEdges[halfEdges[it].nextEdge].vertex == currStartVertex }
            if (prev != null) {
                chain.add(0, prev)
                remainingEdges.remove(prev)
                curr = prev
            } else {
                break
            }
        }

        chains.add(chain)
    }

    val newFaceIds = mutableSetOf<Int>()

    for (chain in chains) {
        val isClosed = if (chain.size > 1) {
            halfEdges[halfEdges[chain.last()].nextEdge].vertex == halfEdges[chain.first()].vertex
        } else {
            halfEdges[chain.last()].nextEdge == chain.first()
        }

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

        data class OffsetResult(val vOffset0: Vector3, val vOffset1: Vector3, val isJoin: Boolean)

        val vertexOffsetResults = mutableListOf<OffsetResult>()

        fun computeOffset(nPrev: Vector3, nCurr: Vector3, edgeIdx: Int): OffsetResult {
            val dot = nPrev.dot(nCurr).coerceIn(-1.0, 1.0)
            val cross = nPrev.cross(nCurr)

            // Try to find a face normal to determine "up" for THIS specific corner
            var up = Vector3.UNIT_Z
            val edge = halfEdges[edgeIdx]
            if (edge.face != -1) {
                val f = faces[edge.face]
                val e0 = halfEdges[f.edge]
                val vA = vertices[e0.vertex].position
                val vB = vertices[halfEdges[e0.nextEdge].vertex].position
                val vC = vertices[halfEdges[halfEdges[e0.nextEdge].nextEdge].vertex].position
                val fn = (vB - vA).cross(vC - vB).normalized
                if (fn.length > 0.1) {
                    up = fn
                }
            }
            
            val actualIsOuter = cross.dot(up) < -1e-6
            
            // For joins, we check if it's an outer corner based on THIS edge's face normal.
            if (useJoins && actualIsOuter && offset > 0.0) {
                return OffsetResult(nPrev * offset, nCurr * offset, true)
            } else if (useJoins && !actualIsOuter && offset < 0.0) {
                 return OffsetResult(nPrev * offset, nCurr * offset, true)
            } else {
                val vOffset = (nPrev + nCurr).normalized
                val cosHalfAngle = kotlin.math.sqrt((1.0 + dot) / 2.0)
                val length = if (cosHalfAngle > 0.1) 1.0 / cosHalfAngle else 1.0
                val finalOffset = vOffset * length * offset
                return OffsetResult(finalOffset, finalOffset, false)
            }
        }

        if (isClosed) {
            for (i in chain.indices) {
                val nPrev = edgeNormals[(i + chain.size - 1) % chain.size]
                val nCurr = edgeNormals[i]
                vertexOffsetResults.add(computeOffset(nPrev, nCurr, chain[i]))
            }
        } else {
            // First vertex
            vertexOffsetResults.add(OffsetResult(edgeNormals.first() * offset, edgeNormals.first() * offset, false))
            // Middle vertices
            for (i in 1 until chain.size) {
                val nPrev = edgeNormals[i - 1]
                val nCurr = edgeNormals[i]
                vertexOffsetResults.add(computeOffset(nPrev, nCurr, chain[i]))
            }
            // Last vertex
            vertexOffsetResults.add(OffsetResult(edgeNormals.last() * offset, edgeNormals.last() * offset, false))
        }

        // 3. Create new vertices and faces
        val originalVertexIndices = mutableListOf<Int>()
        for (eIdx in chain) {
            originalVertexIndices.add(halfEdges[eIdx].vertex)
        }
        if (!isClosed) {
            originalVertexIndices.add(halfEdges[halfEdges[chain.last()].nextEdge].vertex)
        }

        val newVertexIndices = mutableListOf<Pair<Int, Int>>()
        for (i in originalVertexIndices.indices) {
            val vIdx = originalVertexIndices[i]
            val res = vertexOffsetResults[i]
            
            val nv0 = vertices.size
            vertices.add(Vertex(vertices[vIdx].position + res.vOffset0, -1))
            
            val nv1 = if (res.isJoin) {
                val idx = vertices.size
                vertices.add(Vertex(vertices[vIdx].position + res.vOffset1, -1))
                idx
            } else {
                nv0
            }
            newVertexIndices.add(Pair(nv0, nv1))
        }

        // For each edge in chain, create a quad face
        val edgeFaceIds = mutableListOf<Int>()
        for (i in chain.indices) {
            val eIdx = chain[i]
            
            val v0 = originalVertexIndices[i]
            val v1 = originalVertexIndices[(i + 1) % originalVertexIndices.size]
            
            val nv0 = newVertexIndices[i].second // Use the SECOND vertex of the start corner
            val nv1 = newVertexIndices[(i + 1) % newVertexIndices.size].first // Use the FIRST vertex of the end corner

            val fIdx = faces.size
            faces.add(Face(-1))
            newFaceIds.add(fIdx)
            edgeFaceIds.add(fIdx)

            val e0Idx = halfEdges.size
            val e1Idx = halfEdges.size + 1
            val e2Idx = halfEdges.size + 2
            val e3Idx = halfEdges.size + 3

            val e0 = HalfEdge(fIdx, nv0, e1Idx, e3Idx, -1, halfEdges[eIdx].attributes.copyOf())
            val e1 = HalfEdge(fIdx, nv1, e2Idx, e0Idx, -1, halfEdges[eIdx].attributes.copyOf())
            val e2 = HalfEdge(fIdx, v1, e3Idx, e1Idx, eIdx, halfEdges[eIdx].attributes.copyOf())
            val e3 = HalfEdge(fIdx, v0, e0Idx, e2Idx, -1, halfEdges[eIdx].attributes.copyOf())

            halfEdges.addAll(listOf(e0, e1, e2, e3))
            faces[fIdx].edge = e0Idx
            halfEdges[eIdx].otherEdge = e2Idx

            if (vertices[nv0].edge == -1) vertices[nv0].edge = e0Idx
            if (vertices[nv1].edge == -1) vertices[nv1].edge = e1Idx
        }

        // Create join faces
        val allFaceIds = mutableListOf<Int>()
        for (i in originalVertexIndices.indices) {
            val res = vertexOffsetResults[i]
            
            // Face before this vertex
            val prevEdgeFaceIdx = if (i > 0) edgeFaceIds[i - 1] else if (isClosed) edgeFaceIds.last() else -1
            // Face after this vertex
            val nextEdgeFaceIdx = if (i < chain.size) edgeFaceIds[i] else if (isClosed) edgeFaceIds.first() else -1

            if (res.isJoin) {
                // Create a triangle join face
                val v0 = originalVertexIndices[i]
                val nv0 = newVertexIndices[i].first
                val nv1 = newVertexIndices[i].second
                
                val fIdx = faces.size
                faces.add(Face(-1))
                newFaceIds.add(fIdx)
                
                val e0Idx = halfEdges.size
                val e1Idx = halfEdges.size + 1
                val e2Idx = halfEdges.size + 2
                
                // e0: nv0 -> nv1 (outer)
                // e1: nv1 -> v0
                // e2: v0 -> nv0
                val e0 = HalfEdge(fIdx, nv0, e1Idx, e2Idx, -1, IntArray(0))
                val e1 = HalfEdge(fIdx, nv1, e2Idx, e0Idx, -1, IntArray(0))
                val e2 = HalfEdge(fIdx, v0, e0Idx, e1Idx, -1, IntArray(0))
                
                halfEdges.addAll(listOf(e0, e1, e2))
                faces[fIdx].edge = e0Idx
                
                // Update vertex.edge if it was -1
                if (vertices[nv0].edge == -1) vertices[nv0].edge = e0Idx
                if (vertices[nv1].edge == -1) vertices[nv1].edge = e1Idx
                
                // Link to neighbors
                if (prevEdgeFaceIdx != -1) {
                    val e1OfPrev = faces[prevEdgeFaceIdx].edge + 1
                    halfEdges[e1OfPrev].otherEdge = e2Idx
                    halfEdges[e2Idx].otherEdge = e1OfPrev
                }
                if (nextEdgeFaceIdx != -1) {
                    val e3OfNext = faces[nextEdgeFaceIdx].edge + 3
                    halfEdges[e3OfNext].otherEdge = e1Idx
                    halfEdges[e1Idx].otherEdge = e3OfNext
                }
            } else {
                // No join, link quads directly
                if (prevEdgeFaceIdx != -1 && nextEdgeFaceIdx != -1) {
                    val e1OfPrev = faces[prevEdgeFaceIdx].edge + 1
                    val e3OfNext = faces[nextEdgeFaceIdx].edge + 3
                    halfEdges[e1OfPrev].otherEdge = e3OfNext
                    halfEdges[e3OfNext].otherEdge = e1OfPrev
                }
            }
        }
    }

    return newFaceIds
}