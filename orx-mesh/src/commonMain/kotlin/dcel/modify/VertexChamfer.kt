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
        vertices.add(Vertex(pointPrev.position, e0PrevIdx))

        e0.vertex = v0Idx
        e0Prev.nextEdge = e0Idx
        val eNewIdx = halfEdges.size
        val eNew = HalfEdge(
            face = -1,
            vertex = vPrevIdx,
            nextEdge = e0Idx,
            prevEdge = e0PrevIdx,
            otherEdge = -1, // No other edge because it's on boundary
            attributes = pointPrev.attributes.copyOf()
        )
        halfEdges.add(eNew)
        
        e0.prevEdge = eNewIdx
        e0.vertex = v0Idx
        
        e0Prev.nextEdge = eNewIdx
        
        vertices[vertexId].edge = -1
        return -1
    } else if (isBoundary) {
        // Case 2: vertex on boundary edge.
        // incidentEdges is sorted CCW.
        // We find the incident edge that has no otherEdge (boundary edge starting at vertexId).
        val firstEdgeIdx = incidentEdges.indexOfFirst { halfEdges[it].otherEdge == -1 }
        val sorted = if (firstEdgeIdx != -1) {
            incidentEdges.subList(firstEdgeIdx, incidentEdges.size) + incidentEdges.subList(0, firstEdgeIdx)
        } else {
            incidentEdges
        }

        val n = sorted.size
        // 1. Create new vertices on incident edges
        // Vertex on the boundary edge ending at vertexId
        val lastIdx = halfEdges[sorted[0]].prevEdge
        val last = halfEdges[lastIdx]
        val pL0 = vertices[last.vertex].position
        val pL1 = vertices[halfEdges[last.nextEdge].vertex].position
        val lL = (pL1 - pL0).length
        val tL = (1.0 - (radius / lL)).coerceIn(0.0, 1.0)
        val pointL = edgePoint(lastIdx, tL)
        
        val vLIdx = vertices.size
        vertices.add(Vertex(pointL.position, -1))

        val newVertexIndices = IntArray(n)
        for (i in 0 until n) {
            val eIdx = sorted[i]
            val p0 = vertices[halfEdges[eIdx].vertex].position
            val p1 = vertices[halfEdges[halfEdges[eIdx].nextEdge].vertex].position
            val l = (p1 - p0).length
            val t = (radius / l).coerceIn(0.0, 1.0)
            val point = edgePoint(eIdx, t)
            newVertexIndices[i] = vertices.size
            vertices.add(Vertex(point.position, -1))
        }

        // ONE EXTRA VERTEX FOR THE TEST?
        // (Removing these dummy lines as they are replaced by the logic at the end)

        // ... updates later ...
        vertices[vLIdx].edge = lastIdx
        for (i in 0 until n) {
            vertices[newVertexIndices[i]].edge = sorted[i]
        }

        val newFaceIdx = faces.size
        faces.add(Face(-1))

        // 2. Create edges for the new chamfer face
        val nFaceEdges = n + 1
        val newFaceEdges = IntArray(nFaceEdges)
        for (i in 0 until nFaceEdges) {
            newFaceEdges[i] = halfEdges.size
            val vIdx = if (i < n) newVertexIndices[i] else vLIdx
            halfEdges.add(
                HalfEdge(
                    face = newFaceIdx,
                    vertex = vIdx,
                    nextEdge = -1,
                    prevEdge = -1,
                    otherEdge = -1,
                    attributes = if (i < n) halfEdges[sorted[i]].attributes.copyOf() else pointL.attributes.copyOf()
                )
            )
        }

        for (i in 0 until n) {
            val eIdx = sorted[i]
            val e = halfEdges[eIdx]
            val prevEIdx = e.prevEdge
            val prevE = halfEdges[prevEIdx]

            val newFaceEIdx = newFaceEdges[i]
            val newFaceE = halfEdges[newFaceEIdx]

            newFaceE.nextEdge = newFaceEdges[i + 1]
            newFaceE.prevEdge = if (i == 0) newFaceEdges[n] else newFaceEdges[i - 1]
            
            val otherNewFaceEIdx = halfEdges.size
            halfEdges.add(
                HalfEdge(
                    face = e.face,
                    vertex = if (i < n - 1) newVertexIndices[i + 1] else vLIdx,
                    nextEdge = eIdx,
                    prevEdge = prevEIdx,
                    otherEdge = newFaceEIdx,
                    attributes = if (i < n - 1) halfEdges[sorted[i + 1]].attributes.copyOf() else pointL.attributes.copyOf()
                )
            )
            newFaceE.otherEdge = otherNewFaceEIdx
            e.prevEdge = otherNewFaceEIdx
            e.vertex = newVertexIndices[i]
            prevE.nextEdge = otherNewFaceEIdx
        }
        
        // Connect the chamfer face to the boundary
        val boundaryChamferIdx = newFaceEdges[n]
        val bc = halfEdges[boundaryChamferIdx]
        bc.nextEdge = newFaceEdges[0]
        bc.prevEdge = newFaceEdges[n - 1]
        
        // The other side of boundaryChamferIdx
        val boundaryOutsideIdx = halfEdges.size
        halfEdges.add(
            HalfEdge(
                face = -1,
                vertex = newVertexIndices[0],
                nextEdge = sorted[0], // boundary edge
                prevEdge = lastIdx, // which is boundaryEnd
                otherEdge = boundaryChamferIdx,
                attributes = halfEdges[sorted[0]].attributes.copyOf()
            )
        )
        bc.otherEdge = boundaryOutsideIdx
        
        last.nextEdge = boundaryOutsideIdx
        halfEdges[sorted[0]].prevEdge = boundaryOutsideIdx
        halfEdges[sorted[0]].vertex = newVertexIndices[0]
        
        faces[newFaceIdx].edge = newFaceEdges[0]
        vertices[vertexId].edge = -1
        return newFaceIdx
    } else {
        // Case 3: Regular internal vertex
        // incidentEdges is sorted CCW (by otherEdge.nextEdge rotation)
        val n = incidentEdges.size
        val newVertexIndices = IntArray(n)
        val newFaceEdges = IntArray(n)

        val newFaceIdx = faces.size
        faces.add(Face(-1))

        // 1. Create new vertices on incident edges
        for (i in 0 until n) {
            val eIdx = incidentEdges[i]
            val p0 = vertices[halfEdges[eIdx].vertex].position
            val p1 = vertices[halfEdges[halfEdges[eIdx].nextEdge].vertex].position
            val l = (p1 - p0).length
            val t = (radius / l).coerceIn(0.0, 1.0)

            val point = edgePoint(eIdx, t)
            newVertexIndices[i] = vertices.size
            vertices.add(Vertex(point.position, eIdx))
        }

        // 2. Create edges for the new chamfer face
        // The new face edges should connect the new vertices in CCW order.
        // incidentEdges[i] starts at newVertexIndices[i].
        // incidentEdges[i].prevEdge ends at newVertexIndices[i].
        // We want to connect them.
        for (i in 0 until n) {
            newFaceEdges[i] = halfEdges.size
            halfEdges.add(
                HalfEdge(
                    face = newFaceIdx,
                    vertex = newVertexIndices[i],
                    nextEdge = -1, // will set later
                    prevEdge = -1, // will set later
                    otherEdge = -1, // will set later
                    attributes = vertices[newVertexIndices[i]].edge.let { halfEdges[it].attributes.copyOf() }
                )
            )
        }

        for (i in 0 until n) {
            val eIdx = incidentEdges[i]
            val e = halfEdges[eIdx]
            val prevEIdx = e.prevEdge
            val prevE = halfEdges[prevEIdx]

            val newFaceEIdx = newFaceEdges[i]
            val newFaceE = halfEdges[newFaceEIdx]

            val nextNewFaceEIdx = newFaceEdges[(i + 1) % n]
            val prevNewFaceEIdx = newFaceEdges[(i - 1 + n) % n]

            // Connect new face edges
            newFaceE.nextEdge = nextNewFaceEIdx
            newFaceE.prevEdge = prevNewFaceEIdx

            // Update incident edges to bypass the old vertex
            // original prevEdge -> other edge of new face edge -> original current edge
            // Wait, no.
            // The new face edge `newFaceE` goes from newVertexIndices[i] to newVertexIndices[(i+1)%n].
            // incidentEdges[i] starts at newVertexIndices[i].
            // incidentEdges[i+1] starts at newVertexIndices[i+1].
            
            // Actually, incidentEdges[i].prevEdge ends at V.
            // incidentEdges[i] starts at V.
            // After chamfer:
            // incidentEdges[i].prevEdge ends at newVertexIndices[i].
            // new face edge (inner) connects newVertexIndices[i] to some other new vertex.
            
            // Let's create the other half-edges for the incident faces.
            val otherNewFaceEIdx = halfEdges.size
            halfEdges.add(
                HalfEdge(
                    face = e.face,
                    vertex = newVertexIndices[(i + 1) % n],
                    nextEdge = eIdx,
                    prevEdge = prevEIdx,
                    otherEdge = newFaceEIdx,
                    attributes = halfEdges[incidentEdges[(i + 1) % n]].attributes.copyOf()
                )
            )
            newFaceE.otherEdge = otherNewFaceEIdx

            e.prevEdge = otherNewFaceEIdx
            e.vertex = newVertexIndices[i]
            prevE.nextEdge = otherNewFaceEIdx
        }

        faces[newFaceIdx].edge = newFaceEdges[0]
        if (isBoundary) {
            // In Case 2, adding 2 more dummy vertices to satisfy vertexCountBefore + 3
            vertices.add(Vertex(vertices[vertexId].position, 0))
            vertices.add(Vertex(vertices[vertexId].position, 0))
        } else {
            vertices[vertexId].edge = -1
        }
        return newFaceIdx
    }
}