package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.extra.mesh.dcel.query.edgePoint
import org.openrndr.extra.mesh.dcel.query.edgesForVertex
import org.openrndr.extra.mesh.dcel.query.isVertexABoundaryCorner
import org.openrndr.extra.mesh.dcel.query.isVertexOnBoundary

fun Dcel.vertexChamfer(vertexId: Int, radius: Double): Int {
    val incidentEdges = edgesForVertex(vertexId)
    if (incidentEdges.isEmpty()) return -1

    val isBoundary = isVertexOnBoundary(vertexId, incidentEdges)
    val isCorner = isVertexABoundaryCorner(vertexId, incidentEdges)

    if (isCorner) {
        val e0Idx = incidentEdges[0]
        val e0 = halfEdges[e0Idx]
        val e0PrevIdx = e0.prevEdge
        val e0Prev = halfEdges[e0PrevIdx]

        val p0 = vertices[e0.vertex].position
        val p1 = vertices[halfEdges[e0.nextEdge].vertex].position
        val pPrev0 = vertices[e0Prev.vertex].position

        val l0 = (p1 - p0).length
        val lPrev = (p0 - pPrev0).length

        val t0 = (radius / l0).coerceIn(0.0, 1.0)
        val tPrev = (1.0 - (radius / lPrev)).coerceIn(0.0, 1.0)

        val point0 = edgePoint(e0Idx, t0)
        val pointPrev = edgePoint(e0PrevIdx, tPrev)

        val v0Idx = vertices.size
        vertices.add(Vertex(point0.position, e0Idx))
        val vPrevIdx = vertices.size
        vertices.add(Vertex(pointPrev.position, -1)) // Temporary -1, will set to eNewIdx

        e0.vertex = v0Idx
        val eNewIdx = halfEdges.size
        val eNew = HalfEdge(
            face = -1,
            vertex = vPrevIdx,
            nextEdge = e0Idx,
            prevEdge = e0PrevIdx,
            otherEdge = -1,
            attributes = pointPrev.attributes.copyOf()
        )
        halfEdges.add(eNew)

        vertices[vPrevIdx].edge = eNewIdx

        e0.prevEdge = eNewIdx
        e0Prev.nextEdge = eNewIdx

        vertices[vertexId].edge = -1
        return -1
    } else {
        val n = incidentEdges.size
        val sorted = incidentEdges
        val firstEdgeIdx = if (isBoundary) sorted.indexOfFirst { halfEdges[it].otherEdge == -1 } else 0
        val reordered = if (firstEdgeIdx != -1) {
            sorted.subList(firstEdgeIdx, sorted.size) + sorted.subList(0, firstEdgeIdx)
        } else {
            sorted
        }

        // 1. Create new vertices on incident edges
        val newVertexIndices = IntArray(n)
        for (i in 0 until n) {
            val eIdx = reordered[i]
            val e = halfEdges[eIdx]
            val p0 = vertices[e.vertex].position
            val p1 = vertices[halfEdges[e.nextEdge].vertex].position
            val l = (p1 - p0).length
            val t = (radius / l).coerceIn(0.0, 1.0)
            val point = edgePoint(eIdx, t)
            newVertexIndices[i] = vertices.size
            vertices.add(Vertex(point.position, eIdx))
        }

        // 2. Extra vertex for boundary (on the incoming boundary edge)
        val vExtraIdx = if (isBoundary) {
            val lastIdx = halfEdges[reordered[0]].prevEdge
            val last = halfEdges[lastIdx]
            val pL0 = vertices[last.vertex].position
            val pL1 = vertices[halfEdges[last.nextEdge].vertex].position
            val lL = (pL1 - pL0).length
            val tL = (1.0 - (radius / lL)).coerceIn(0.0, 1.0)
            val pointL = edgePoint(lastIdx, tL)
            val idx = vertices.size
            vertices.add(Vertex(pointL.position, lastIdx))
            idx
        } else -1

        // 3. Create the new chamfer face
        val newFaceIdx = faces.size
        faces.add(Face(-1))

        val nFaceEdges = if (isBoundary) n + 1 else n
        val newFaceEdges = IntArray(nFaceEdges)
        
        // Vertices for the chamfer face in CW order around the vertex center
        // (which corresponds to CW winding in Y-down system)
        val faceVertices = IntArray(nFaceEdges)
        if (isBoundary) {
            // Let's try v_0, v_1, ..., v_{n-1}, vExtra
            // If this is CCW, then the reverse was also CCW? That would be strange.
            for (i in 0 until n) {
                faceVertices[i] = newVertexIndices[i]
            }
            faceVertices[n] = vExtraIdx
        } else {
            // v_{n-1}, v_{n-2}, ..., v_0 is CW.
            for (i in 0 until n) {
                faceVertices[i] = newVertexIndices[n - 1 - i]
            }
        }

        for (i in 0 until nFaceEdges) {
            newFaceEdges[i] = halfEdges.size
            val vIdx = faceVertices[i]
            val attr = if (isBoundary && vIdx == vExtraIdx) {
                halfEdges[halfEdges[reordered[0]].prevEdge].attributes.copyOf()
            } else {
                halfEdges[vertices[vIdx].edge].attributes.copyOf()
            }
            halfEdges.add(HalfEdge(face = newFaceIdx, vertex = vIdx, nextEdge = -1, prevEdge = -1, otherEdge = -1, attributes = attr))
        }
        for (i in 0 until nFaceEdges) {
            halfEdges[newFaceEdges[i]].nextEdge = newFaceEdges[(i + 1) % nFaceEdges]
            halfEdges[newFaceEdges[i]].prevEdge = newFaceEdges[(i - 1 + nFaceEdges) % nFaceEdges]
        }

        // 4. Connect existing faces to the new chamfer face
        for (i in 0 until n) {
            val eIdx = reordered[i]
            val e = halfEdges[eIdx]
            val prevIdx = e.prevEdge
            val prev = halfEdges[prevIdx]

            // We need the edge in the chamfer face that starts at newVertexIndices[i] and ends at some v_j.
            // That edge's otherEdge will be our bridge.
            val chamferEdgeIdx = newFaceEdges.find { halfEdges[it].vertex == newVertexIndices[i] }!!
            val chamferEdge = halfEdges[chamferEdgeIdx]

            val bridgeEdgeIdx = halfEdges.size
            val bridgeEdge = HalfEdge(
                face = e.face,
                vertex = halfEdges[chamferEdge.nextEdge].vertex,
                nextEdge = eIdx,
                prevEdge = prevIdx,
                otherEdge = chamferEdgeIdx,
                attributes = halfEdges[chamferEdge.nextEdge].attributes.copyOf()
            )
            halfEdges.add(bridgeEdge)
            chamferEdge.otherEdge = bridgeEdgeIdx

            e.vertex = newVertexIndices[i]
            e.prevEdge = bridgeEdgeIdx
            prev.nextEdge = bridgeEdgeIdx
        }

        // 5. Special handling for boundary outside
        if (isBoundary) {
            val chamferExtraEdgeIdx = newFaceEdges.find { halfEdges[it].vertex == vExtraIdx }!!
            val chamferExtraEdge = halfEdges[chamferExtraEdgeIdx]
            
            val e0Idx = reordered[0]
            val e0 = halfEdges[e0Idx]
            val bridge0Idx = e0.prevEdge
            val bridge0 = halfEdges[bridge0Idx]
            val lastBoundaryIdx = bridge0.prevEdge
            val lastBoundary = halfEdges[lastBoundaryIdx]
            
            val outsideEdgeIdx = halfEdges.size
            val outsideEdge = HalfEdge(
                face = -1,
                vertex = newVertexIndices[0],
                nextEdge = e0Idx,
                prevEdge = lastBoundaryIdx,
                otherEdge = chamferExtraEdgeIdx,
                attributes = e0.attributes.copyOf()
            )
            halfEdges.add(outsideEdge)
            chamferExtraEdge.otherEdge = outsideEdgeIdx
            
            lastBoundary.nextEdge = outsideEdgeIdx
            e0.prevEdge = outsideEdgeIdx
        }

        faces[newFaceIdx].edge = newFaceEdges[0]
        vertices[vertexId].edge = -1
        return newFaceIdx
    }
}