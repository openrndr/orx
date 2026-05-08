package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.query.edgePoint

fun Dcel.edgeSetChamfer(edgeIds: Set<Int>, radius: Double): Set<Int> {
    val newFaceIds = mutableSetOf<Int>()

    val processedEdges = mutableSetOf<Int>()

    for (eIdx in edgeIds) {
        if (eIdx in processedEdges) continue
        
        val edge = halfEdges[eIdx]
        val oeIdx = edge.otherEdge
        
        if (oeIdx != -1 && oeIdx in edgeIds) {
            processedEdges.add(oeIdx)
        }
        processedEdges.add(eIdx)

        val v0Idx = edge.vertex
        val vNextIdx = halfEdges[edge.nextEdge].vertex
        
        val p0 = vertices[v0Idx].position
        val pNext = vertices[vNextIdx].position
        val l = (pNext - p0).length
        if (l < 1e-6) continue
        
        val t0 = (radius / l).coerceIn(0.0, 0.5)
        val t1 = (1.0 - radius / l).coerceIn(0.5, 1.0)
        
        val point0 = edgePoint(eIdx, t0)
        val point1 = edgePoint(eIdx, t1)
        
        val nv0Idx = vertices.size
        vertices.add(Vertex(point0.position, -1))
        val nv1Idx = vertices.size
        vertices.add(Vertex(point1.position, -1))
        
        val eNextIdx = edge.nextEdge
        val eMidIdx = halfEdges.size
        val eEndIdx = halfEdges.size + 1
        
        val eMid = HalfEdge(edge.face, nv0Idx, eEndIdx, eIdx, -1, point0.attributes.copyOf())
        val eEnd = HalfEdge(edge.face, nv1Idx, eNextIdx, eMidIdx, -1, point1.attributes.copyOf())
        
        halfEdges.add(eMid)
        halfEdges.add(eEnd)
        
        edge.nextEdge = eMidIdx
        halfEdges[eNextIdx].prevEdge = eEndIdx
        
        vertices[nv0Idx].edge = eMidIdx
        vertices[nv1Idx].edge = eEndIdx
        
        if (oeIdx != -1) {
            val oe = halfEdges[oeIdx]
            val oeNextIdx = oe.nextEdge
            
            val m0Idx = vertices.size
            vertices.add(Vertex(point0.position, -1))
            val m1Idx = vertices.size
            vertices.add(Vertex(point1.position, -1))
            
            val oeMidIdx = halfEdges.size
            val oeEndIdx = halfEdges.size + 1
            
            val oeMid = HalfEdge(oe.face, m1Idx, oeEndIdx, oeIdx, -1, point1.attributes.copyOf())
            val oeEnd = HalfEdge(oe.face, m0Idx, oeNextIdx, oeMidIdx, -1, point0.attributes.copyOf())
            
            halfEdges.add(oeMid)
            halfEdges.add(oeEnd)
            
            oe.nextEdge = oeMidIdx
            halfEdges[oeNextIdx].prevEdge = oeEndIdx
            
            vertices[m1Idx].edge = oeMidIdx
            vertices[m0Idx].edge = oeEndIdx
            
            val fIdx = faces.size
            faces.add(Face(-1))
            newFaceIds.add(fIdx)
            
            val ce0Idx = halfEdges.size
            val ce1Idx = halfEdges.size + 1
            val ce2Idx = halfEdges.size + 2
            val ce3Idx = halfEdges.size + 3
            
            val ce0 = HalfEdge(fIdx, nv0Idx, ce1Idx, ce3Idx, -1, point0.attributes.copyOf())
            val ce1 = HalfEdge(fIdx, m0Idx, ce2Idx, ce0Idx, oeMidIdx, point0.attributes.copyOf())
            val ce2 = HalfEdge(fIdx, m1Idx, ce3Idx, ce1Idx, -1, point1.attributes.copyOf())
            val ce3 = HalfEdge(fIdx, nv1Idx, ce0Idx, ce2Idx, eMidIdx, point1.attributes.copyOf())
            
            halfEdges.add(ce0)
            halfEdges.add(ce1)
            halfEdges.add(ce2)
            halfEdges.add(ce3)
            
            eMid.otherEdge = ce3Idx
            oeMid.otherEdge = ce1Idx
            
            faces[fIdx].edge = ce0Idx
        }
    }

    return newFaceIds
}