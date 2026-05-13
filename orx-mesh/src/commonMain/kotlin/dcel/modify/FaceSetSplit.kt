package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Vector3
import kotlin.math.abs

fun Dcel.faceSetSplit(faceIds: Set<Int>, plane: Plane, splitEpsilon: Double = 1E-6): Set<Int> {
    val activeFaceIds = faceIds.toMutableSet()
    val processedEdges = mutableMapOf<Int, Int>() // edgeIndex to newEdgeIndex (the one starting at the new vertex)

    val faceIdsToProcess = faceIds.toList()

    for (faceId in faceIdsToProcess) {
        val edges = edgesForFace(faceId)
        val verticesOnPlane = mutableListOf<Int>()

        for (eIdx in edges) {
            val edge = halfEdges[eIdx]
            val v0Idx = edge.vertex
            val v1Idx = halfEdges[edge.nextEdge].vertex

            val p0 = vertices[v0Idx].position
            val p1 = vertices[v1Idx].position

            val s0 = plane.side(p0)
            val s1 = plane.side(p1)

            if (abs(s0) < splitEpsilon) {
                if (verticesOnPlane.none { vertices[it].position.distanceTo(p0) < splitEpsilon }) {
                    verticesOnPlane.add(v0Idx)
                }
            } else if (s0 * s1 < 0) {
                // Crossing
                val existingSplit = processedEdges[eIdx] ?: processedEdges[halfEdges[eIdx].otherEdge]
                if (existingSplit != null) {
                    val vNewIdx = halfEdges[existingSplit].vertex
                    val pNew = vertices[vNewIdx].position
                    if (verticesOnPlane.none { vertices[it].position.distanceTo(pNew) < splitEpsilon }) {
                        verticesOnPlane.add(vNewIdx)
                    }
                } else {
                    val t = s0 / (s0 - s1)
                    val newEdgeIdx = edgeSplitAt(eIdx, t)
                    val vNewIdx = halfEdges[newEdgeIdx].vertex
                    processedEdges[eIdx] = newEdgeIdx
                    val pNew = vertices[vNewIdx].position
                    if (verticesOnPlane.none { vertices[it].position.distanceTo(pNew) < splitEpsilon }) {
                        verticesOnPlane.add(vNewIdx)
                    }
                }
            }
        }

        if (verticesOnPlane.size == 2) {
            val v0 = verticesOnPlane[0]
            val v1 = verticesOnPlane[1]

            // Find half-edges in this face that start at these vertices (or near them)
            val faceEdges = edgesForFace(faceId)
            val e0 = faceEdges.find { 
                val v = halfEdges[it].vertex
                v == v0 || vertices[v].position.distanceTo(vertices[v0].position) < splitEpsilon 
            }
            val e1 = faceEdges.find { 
                val v = halfEdges[it].vertex
                v == v1 || vertices[v].position.distanceTo(vertices[v1].position) < splitEpsilon 
            }

            if (e0 != null && e1 != null) {
                val beforeFaces = faces.size
                edgeInsert(e0, e1)
                val afterFaces = faces.size
                if (afterFaces > beforeFaces) {
                    activeFaceIds.add(afterFaces - 1)
                }
            }
        }
    }

    return activeFaceIds
}

//fun Dcel.faceSetSplit(faceIds: Set<Int>, polygon: List<Vector3>, perpendicular: Vector3 = Vector3(0.0, 0.0, 1.0), splitEpsilon: Double = 1E-6): Set<Int> {
//    // perform polygon-polygon clipping against the faces in faceIds and polygon.
//    // the segments of the polygon are to be considered edge-wise planar strips.
//    // the planar strips are in one direction bounded by the edges and infinte in the perpendicular direction
//
//    // return the new faces created by the split
//
//}
