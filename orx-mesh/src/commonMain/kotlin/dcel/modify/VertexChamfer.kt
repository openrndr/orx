package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.extra.mesh.dcel.query.edgePoint
import org.openrndr.extra.mesh.dcel.query.edgesForVertex

fun Dcel.vertexChamfer(vertexId: Int, radius: Double): Int {
    if (vertexId < 0 || vertexId >= vertices.size) return -1

    val outgoing = edgesForVertex(vertexId)
    if (outgoing.isEmpty()) return -1
    val n = outgoing.size

    val vSplit = IntArray(n) { -1 }

    // 1. Split each outgoing edge.
    // We only need to find the split points on each outgoing edge.
    for (i in 0 until n) {
        val eIdx = outgoing[i]
        val e = halfEdges[eIdx]
        val v0 = vertices[e.vertex].position
        val v1 = vertices[halfEdges[e.nextEdge].vertex].position
        val length = (v1 - v0).length
        val t = if (length > 1e-6) (radius / length).coerceIn(0.0, 1.0) else 0.0
        val point = edgePoint(eIdx, t)

        val vIdx = vertices.size
        vertices.add(Vertex(point.position, -1))
        vSplit[i] = vIdx
    }

    // 2. Create the chamfer face and bridges
    val chamferFaceIdx = faces.size
    faces.add(Face(-1))
    
    // Track chamfer edges to link them later
    val chamferEdges = IntArray(n) { -1 }

    for (i in 0 until n) {
        val eIdx = outgoing[i]
        val fIdx = halfEdges[eIdx].face

        // eIdx goes from vertexId to V_next.
        // In face fIdx, there's an edge prevIdx that ends at vertexId.
        val prevIdx = halfEdges[eIdx].prevEdge
        if (prevIdx == -1) continue

        // Find which vSplit corresponds to the start of the chamfer edge in this face.
        // The edge before eIdx in the vertex rotation (counter-clockwise) 
        // is the one whose otherEdge is the prevEdge of eIdx.
        val oePrevIdx = halfEdges[prevIdx].otherEdge
        val j = if (oePrevIdx != -1) outgoing.indexOf(oePrevIdx) else -1
        
        val vStart = if (j != -1) vSplit[j] else -1
        val vEnd = vSplit[i]

        if (vStart != -1) {
            // Create bridge in face fIdx: vStart -> vEnd
            val bridgeIdx = halfEdges.size
            val bridge = HalfEdge(fIdx, vStart, eIdx, prevIdx, -1, halfEdges[eIdx].attributes.copyOf())
            halfEdges.add(bridge)

            // Update fIdx connectivity
            halfEdges[prevIdx].nextEdge = bridgeIdx
            halfEdges[eIdx].prevEdge = bridgeIdx
            
            // eIdx now starts at vStart instead of original vertexId
            halfEdges[eIdx].vertex = vStart
            if (vertices[vStart].edge == -1) vertices[vStart].edge = bridgeIdx

            if (fIdx != -1) {
                // Create chamfer edge: vEnd -> vStart (opposite to bridge)
                val cEdgeIdx = halfEdges.size
                val cEdge = HalfEdge(chamferFaceIdx, vEnd, -1, -1, bridgeIdx, halfEdges[eIdx].attributes.copyOf())
                halfEdges.add(cEdge)
                halfEdges[bridgeIdx].otherEdge = cEdgeIdx
                chamferEdges[i] = cEdgeIdx
            }
        } else {
            // Boundary case: eIdx starts at the boundary without a previous interior face.
            halfEdges[eIdx].vertex = vEnd
            if (vertices[vEnd].edge == -1) vertices[vEnd].edge = eIdx
        }
    }

    // 3. Link chamfer edges
    var firstC = -1
    for (i in 0 until n) {
        val currC = chamferEdges[i]
        if (currC == -1) continue
        if (firstC == -1) firstC = currC
        
        // Find next chamfer edge. 
        // In the chamfer face, it should go from vStart(of i) to ...
        // Wait, cEdge[i] goes vEnd[i] -> vStart[i].
        // The next edge should start at vStart[i].
        // vStart[i] is vSplit[j] where outgoing[j].otherEdge.nextEdge == outgoing[i].
        // So the edge starting at vStart[i] is cEdge[?]
        
        // Let's find j such that vEnd[j] == vStart[i].
        // vStart[i] = vSplit[prev_in_rotation].
        // So we need cEdge[prev_in_rotation].
        
        val eIdx = outgoing[i]
        val prevIdx = halfEdges[eIdx].prevEdge // This is the bridge we just added
        val realPrevIdx = halfEdges[prevIdx].prevEdge // This is the original prevEdge
        val oeRealPrevIdx = halfEdges[realPrevIdx].otherEdge
        val j = outgoing.indexOf(oeRealPrevIdx)
        
        if (j != -1 && chamferEdges[j] != -1) {
            halfEdges[currC].nextEdge = chamferEdges[j]
            halfEdges[chamferEdges[j]].prevEdge = currC
        }
    }

    if (firstC != -1) {
        faces[chamferFaceIdx].edge = firstC
    }

    // 4. Cleanup original vertex
    vertices[vertexId].edge = -1
    // The original outgoing edges now start at vSplit points.
    
    return chamferFaceIdx
}