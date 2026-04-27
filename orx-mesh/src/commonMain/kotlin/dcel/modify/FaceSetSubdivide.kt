package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.facePoint

fun Dcel.convexFaceSetSubdivide(faceIds: Set<Int>): Set<Int> {
    // assumes all faces in faceIds are convex and
    // subdivides all faces in faceIds
    // tracks edges that are split to prevent multiple splits of the same edge
    // if a face has 3 vertices, split it into 4 triangles by subdividing the edges
    // if a face has 4 vertices, split it into 4 quadrilaterals by subdividing the edges and inserting a new vertex in the center
    // if a face has more than 4 vertices, insert a vertex in the center, subdivide the edges and create a quad fan
    // returns the new face ids

    val edgeToNewVertex = mutableMapOf<Int, Int>()

    val faceToOriginalEdgeCount = faceIds.associateWith { faceId ->
        val face = faces.getOrNull(faceId) ?: return@associateWith 0
        edgeLoopIndices(face.edge).size
    }

    val allEdges = faceIds.flatMap { faceId ->
        val face = faces.getOrNull(faceId) ?: return@flatMap emptyList<Int>()
        edgeLoopIndices(face.edge)
    }.toSet()

    // First, split all unique edges
    val splitEdges = mutableSetOf<Int>()
    for (eIdx in allEdges) {
        val edge = halfEdges[eIdx]
        val oeIdx = edge.otherEdge

        if (eIdx in splitEdges || (oeIdx != -1 && oeIdx in splitEdges)) {
            continue
        }

        // Split this edge
        edgeSplitAt(eIdx, 0.5)
        
        // After split:
        // eIdx (v0 -> vNew), next is newEIdx (vNew -> v1)
        // oeIdx (v1 -> vNew), next is newOeIdx (vNew -> v0)
        
        val newEIdx = halfEdges[eIdx].nextEdge
        val vNewIdx = halfEdges[newEIdx].vertex
        edgeToNewVertex[eIdx] = vNewIdx
        if (oeIdx != -1) {
            val newOeIdx = halfEdges[oeIdx].nextEdge
            edgeToNewVertex[oeIdx] = vNewIdx
            edgeToNewVertex[newOeIdx] = vNewIdx // Also map the newly created other half-edge
            splitEdges.add(oeIdx)
            splitEdges.add(newOeIdx)
        }
        edgeToNewVertex[newEIdx] = vNewIdx // Also map the newly created half-edge
        splitEdges.add(eIdx)
        splitEdges.add(newEIdx)
    }

    val resultFaceIds = mutableSetOf<Int>()

    for (faceId in faceIds) {
        val face = faces.getOrNull(faceId) ?: continue
        val currentEdges = edgeLoopIndices(face.edge)
        val originalN = faceToOriginalEdgeCount[faceId] ?: 0
        if (originalN < 3) continue

        if (originalN == 3) {
            // Triangle case: split into 4 triangles
            // Original edges were e0, e1, e2. They are now (e0a, e0b), (e1a, e1b), (e2a, e2b).
            
            // Let's re-identify the 3 original edges.
            // An edge was original if its index was in allEdges.
            val originalEdges = currentEdges.filter { it in allEdges }
            if (originalEdges.size != 3) continue

            val e0a = originalEdges[0]
            val e0b = halfEdges[e0a].nextEdge
            val e1a = originalEdges[1]
            val e1b = halfEdges[e1a].nextEdge
            val e2a = originalEdges[2]
            val e2b = halfEdges[e2a].nextEdge

            val m0Idx = edgeToNewVertex[e0a] ?: continue
            val m1Idx = edgeToNewVertex[e1a] ?: continue
            val m2Idx = edgeToNewVertex[e2a] ?: continue

            // Inner triangle (m0, m1, m2)
            val fInner = faces.size
            faces.add(Face(-1))
            resultFaceIds.add(fInner)

            val eInner01 = halfEdges.size
            val eInner12 = halfEdges.size + 1
            val eInner20 = halfEdges.size + 2

            // Tri 0 (v0, m0, m2)
            val f0 = faceId
            resultFaceIds.add(f0)
            val eInner02 = halfEdges.size + 3

            // Tri 1 (v1, m1, m0)
            val f1 = faces.size
            faces.add(Face(-1))
            resultFaceIds.add(f1)
            val eInner10 = halfEdges.size + 4

            // Tri 2 (v2, m2, m1)
            val f2 = faces.size
            faces.add(Face(-1))
            resultFaceIds.add(f2)
            val eInner21 = halfEdges.size + 5

            // Attributes for inner vertices/edges
            val m0Attrs = halfEdges[e0b].attributes.copyOf()
            val m1Attrs = halfEdges[e1b].attributes.copyOf()
            val m2Attrs = halfEdges[e2b].attributes.copyOf()

            halfEdges.add(HalfEdge(fInner, m0Idx, eInner12, eInner20, eInner10, m0Attrs.copyOf()))
            halfEdges.add(HalfEdge(fInner, m1Idx, eInner20, eInner01, eInner21, m1Attrs.copyOf()))
            halfEdges.add(HalfEdge(fInner, m2Idx, eInner01, eInner12, eInner02, m2Attrs.copyOf()))

            // Tri 0: e0a (v0->m0), eInner02 (m0->m2), e2b (m2->v0)
            halfEdges.add(HalfEdge(f0, m0Idx, e2b, e0a, eInner20, m0Attrs.copyOf()))
            val e0aObj = halfEdges[e0a]
            e0aObj.face = f0
            e0aObj.nextEdge = eInner02
            e0aObj.prevEdge = e2b

            val e2bObj = halfEdges[e2b]
            e2bObj.face = f0
            e2bObj.nextEdge = e0a
            e2bObj.prevEdge = eInner02
            faces[f0].edge = e0a

            // Tri 1: e1a (v1->m1), eInner10 (m1->m0), e0b (m0->v1)
            halfEdges.add(HalfEdge(f1, m1Idx, e0b, e1a, eInner01, m1Attrs.copyOf()))
            val e1aObj = halfEdges[e1a]
            e1aObj.face = f1
            e1aObj.nextEdge = eInner10
            e1aObj.prevEdge = e0b

            val e0bObj = halfEdges[e0b]
            e0bObj.face = f1
            e0bObj.nextEdge = e1a
            e0bObj.prevEdge = eInner10
            faces[f1].edge = e1a

            // Tri 2: e2a (v2->m2), eInner21 (m2->m1), e1b (m1->v2)
            halfEdges.add(HalfEdge(f2, m2Idx, e1b, e2a, eInner12, m2Attrs.copyOf()))
            val e2aObj = halfEdges[e2a]
            e2aObj.face = f2
            e2aObj.nextEdge = eInner21
            e2aObj.prevEdge = e1b

            val e1bObj = halfEdges[e1b]
            e1bObj.face = f2
            e1bObj.nextEdge = e2a
            e1bObj.prevEdge = eInner21
            faces[f2].edge = e2a

            faces[fInner].edge = eInner01

        } else {
            // Quad or more: n quads
            val originalEdges = currentEdges.filter { it in allEdges }
            val n = originalEdges.size
            if (n < 4) continue

            val centerPos = convexFaceCenter(faceId)
            val centerPoint = facePoint(faceId, centerPos)

            val finalColorIdx = centerPoint.color?.let { colors.add(it); colors.size - 1 } ?: -1
            val finalTexCoordIdx = centerPoint.textureCoordinate?.let { textureCoordinates.add(it); textureCoordinates.size - 1 } ?: -1
            val finalNormalIdx = centerPoint.normal?.let { normals.add(it); normals.size - 1 } ?: -1
            val finalTangentIdx = centerPoint.tangent?.let { tangents.add(it); tangents.size - 1 } ?: -1
            val finalBitangentIdx = centerPoint.bitangent?.let { bitangents.add(it); bitangents.size - 1 } ?: -1

            val centerVertexIdx = vertices.size
            vertices.add(Vertex(centerPos, -1))

            val centerAttrs = intArrayOf(finalColorIdx, finalTexCoordIdx, finalNormalIdx, finalTangentIdx, finalBitangentIdx)

            // For each original vertex V_i, we form a quad (V_i, M_i, C, M_{i-1})
            // M_i is the midpoint of edge V_i -> V_{i+1}
            // originalEdges[i] was V_i -> V_{i+1}, now it is V_i -> M_i, its next is M_i -> V_{i+1}

            val midpoints = IntArray(n) { i -> edgeToNewVertex[originalEdges[i]] ?: -1 }
            if (midpoints.any { it == -1 }) continue
            val e_vi_mi = IntArray(n) { i -> originalEdges[i] }
            val e_mi_vip1 = IntArray(n) { i -> halfEdges[originalEdges[i]].nextEdge }

            val e_mi_c = IntArray(n) { i -> halfEdges.size + i }
            val e_c_mi = IntArray(n) { i -> halfEdges.size + n + i }

            val faceIndices = IntArray(n)
            faceIndices[0] = faceId
            for (i in 1 until n) {
                faceIndices[i] = faces.size
                faces.add(Face(-1))
            }

            val e_mi_c_indices = IntArray(n) { i -> halfEdges.size + i }
            val e_c_mi_indices = IntArray(n) { i -> halfEdges.size + n + i }

            // Create e2 (M_i -> C)
            for (i in 0 until n) {
                val fIdx = faceIndices[i]
                val m_i_attrs = halfEdges[e_mi_vip1[i]].attributes.copyOf()
                // e2_i is M_i -> C, its next is e3_i, its prev is e1_i
                // its other edge is e3 of NEXT quad: e3_{i+1} = C -> M_i
                val otherIdx = e_c_mi_indices[(i + 1) % n]
                halfEdges.add(HalfEdge(fIdx, midpoints[i], e_c_mi_indices[i], e_vi_mi[i], otherIdx, m_i_attrs))
            }

            // Create e3 (C -> M_{i-1})
            for (i in 0 until n) {
                val fIdx = faceIndices[i]
                val prevIdx = (i - 1 + n) % n
                // e3_i is C -> M_{i-1}, its next is e4_i, its prev is e2_i
                // its other edge is e2 of PREV quad: e2_{i-1} = M_{i-1} -> C
                val otherIdx = e_mi_c_indices[prevIdx]
                halfEdges.add(HalfEdge(fIdx, centerVertexIdx, e_mi_vip1[prevIdx], e_mi_c_indices[i], otherIdx, centerAttrs.copyOf()))
            }

            // Update existing edges e1 and e4
            for (i in 0 until n) {
                val fIdx = faceIndices[i]
                val prevIdx = (i - 1 + n) % n
                resultFaceIds.add(fIdx)

                val e1Idx = e_vi_mi[i]
                val e2Idx = e_mi_c_indices[i]
                val e3Idx = e_c_mi_indices[i]
                val e4Idx = e_mi_vip1[prevIdx]

                val e1 = halfEdges[e1Idx]
                e1.face = fIdx
                e1.nextEdge = e2Idx
                e1.prevEdge = e4Idx

                val e4 = halfEdges[e4Idx]
                e4.face = fIdx
                e4.nextEdge = e1Idx
                e4.prevEdge = e3Idx
                
                faces[fIdx].edge = e1Idx
            }
            vertices[centerVertexIdx].edge = e_c_mi_indices[0]
        }
    }

    return resultFaceIds
}