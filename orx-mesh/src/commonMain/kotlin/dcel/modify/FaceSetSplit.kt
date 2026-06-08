package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Vector3
import kotlin.math.abs

fun Dcel.faceSetSplit(faceIds: Set<Int>, plane: Plane,
                      splitEpsilon: Double = 1E-6,
                      weldEpsilon: Double = 1E-3): Set<Int> {
    val activeFaceIds = faceIds.toMutableSet()
    val processedEdges = mutableMapOf<Int, Int>() // edgeIndex to newEdgeIndex (the one starting at the new vertex)

    val faceIdsToProcess = faceIds.toList()

    for (faceId in faceIdsToProcess) {
        val faceObj = faces[faceId]
        val outerEdges = edgesForFace(faceId)
        val allLoops = listOf(outerEdges) + faceObj.holeEdges.map { edgeLoopIndices(it) }

        val loopIntersections = mutableListOf<Pair<Int, Int>>() // loopIndex to vertexIdx

        for ((loopIdx, loop) in allLoops.withIndex()) {
            for (eIdx in loop) {
                val edge = halfEdges[eIdx]
                val v0Idx = edge.vertex
                val v1Idx = halfEdges[edge.nextEdge].vertex

                val p0 = vertices[v0Idx].position
                val p1 = vertices[v1Idx].position

                val s0 = plane.side(p0)
                val s1 = plane.side(p1)

                if (abs(s0) < splitEpsilon) {
                    if (loopIntersections.none { vertices[it.second].position.distanceTo(p0) < weldEpsilon }) {
                        loopIntersections.add(loopIdx to v0Idx)
                    }
                } else if (abs(s1) < splitEpsilon) {
                    // check v1 too if it is on the plane
                    if (loopIntersections.none { vertices[it.second].position.distanceTo(p1) < weldEpsilon }) {
                        loopIntersections.add(loopIdx to v1Idx)
                    }
                } else if (s0 * s1 < 0) {
                    val existingSplit = processedEdges[eIdx] ?: processedEdges[halfEdges[eIdx].otherEdge]
                    if (existingSplit != null) {
                        val vNewIdx = halfEdges[existingSplit].vertex
                        val pNew = vertices[vNewIdx].position
                        if (loopIntersections.none { vertices[it.second].position.distanceTo(pNew) < weldEpsilon }) {
                            loopIntersections.add(loopIdx to vNewIdx)
                        }
                    } else {
                        val t = s0 / (s0 - s1)
                        val newEdgeIdx = edgeSplitAt(eIdx, t)
                        val vNewIdx = halfEdges[newEdgeIdx].vertex
                        processedEdges[eIdx] = newEdgeIdx
                        val pNew = vertices[vNewIdx].position
                        if (loopIntersections.none { vertices[it.second].position.distanceTo(pNew) < weldEpsilon }) {
                            loopIntersections.add(loopIdx to vNewIdx)
                        }
                    }
                }
            }
        }

        if (loopIntersections.size >= 2) {
            val p0 = vertices[loopIntersections[0].second].position
            val p1 = if (loopIntersections.size > 1) vertices[loopIntersections[1].second].position else p0 + Vector3.UNIT_X
            val direction = (p1 - p0).normalized
            
            val sortedIntersections = loopIntersections.sortedBy { (vertices[it.second].position - p0).dot(direction) }
            
            // Bridge all holes that are crossed by the plane first
            val crossedHoles = sortedIntersections.filter { it.first > 0 }.map { it.first }.distinct()
            
            for (holeLoopIdx in crossedHoles) {
                // Find intersections on this hole
                val holeInters = sortedIntersections.filter { it.first == holeLoopIdx }
                for (hi in holeInters) {
                    val idx = sortedIntersections.indexOf(hi)
                    if (idx > 0) {
                        val prev = sortedIntersections[idx - 1]
                        val v0 = hi.second
                        val v1 = prev.second
                        
                        fun findEdgeInFaceAnyLoop(vIdx: Int, fId: Int): Int? {
                            val f = faces.getOrNull(fId) ?: return null
                            val allE = edgesForFace(fId) + f.holeEdges.flatMap { edgeLoopIndices(it) }
                            return allE.find { 
                                val v = halfEdges[it].vertex
                                v == vIdx || vertices[v].position.distanceTo(vertices[vIdx].position) < weldEpsilon
                            }
                        }
                        
                        val e0 = findEdgeInFaceAnyLoop(v0, faceId)
                        val e1 = findEdgeInFaceAnyLoop(v1, faceId)
                        if (e0 != null && e1 != null && halfEdges[e0].face == halfEdges[e1].face) {
                            var onSameLoop = false
                            var currL = e0
                            do {
                                if (currL == e1) { onSameLoop = true; break }
                                val next = halfEdges[currL!!].nextEdge
                                if (next == -1) break
                                currL = next
                            } while (currL != e0)
                            
                            if (!onSameLoop) {
                                edgeInsert(e0, e1)
                            }
                        }
                    }
                }
            }
            
            // Re-collect faceId active intersections after all bridges
            fun findEdgeInFaceWithHoles2(vIdx: Int, fId: Int): Int? {
                val f = faces.getOrNull(fId) ?: return null
                val allE = edgesForFace(fId) + f.holeEdges.flatMap { edgeLoopIndices(it) }
                return allE.find { 
                    val v = halfEdges[it].vertex
                    v == vIdx || vertices[v].position.distanceTo(vertices[vIdx].position) < weldEpsilon
                }
            }
            
            val activeIntersectionsInFaceId = sortedIntersections.map { it.second }.filter { v -> findEdgeInFaceWithHoles2(v, faceId) != null }
            val finalSorted = activeIntersectionsInFaceId.sortedBy { (vertices[it].position - p0).dot(direction) }

            for (i in 0 until finalSorted.size - 1 step 2) {
                val v0 = finalSorted[i]
                val v1 = finalSorted[i+1]

                fun findEdgeInFaceWithHoles3(vIdx: Int, fId: Int): Int? {
                    val f = faces.getOrNull(fId) ?: return null
                    val allE = edgesForFace(fId) + f.holeEdges.flatMap { edgeLoopIndices(it) }
                    return allE.find { 
                        val v = halfEdges[it].vertex
                        v == vIdx || vertices[v].position.distanceTo(vertices[vIdx].position) < weldEpsilon
                    }
                }

                var e0 = findEdgeInFaceWithHoles3(v0, faceId)
                var e1 = findEdgeInFaceWithHoles3(v1, faceId)
                
                if (e0 != null && e1 != null && halfEdges[e0].face == halfEdges[e1].face) {
                    if (halfEdges[e0].nextEdge != e1 && halfEdges[e1].nextEdge != e0) {
                        val beforeFaces = faces.size
                        edgeInsert(e0, e1)
                        val afterFaces = faces.size
                        if (afterFaces > beforeFaces) {
                            activeFaceIds.add(afterFaces - 1)
                        }
                    }
                } else {
                    for (newFaceId in activeFaceIds) {
                        if (newFaceId == faceId) continue
                        val ne0 = findEdgeInFaceWithHoles3(v0, newFaceId)
                        val ne1 = findEdgeInFaceWithHoles3(v1, newFaceId)
                        if (ne0 != null && ne1 != null && halfEdges[ne0].face == halfEdges[ne1].face) {
                            if (halfEdges[ne0].nextEdge != ne1 && halfEdges[ne1].nextEdge != ne0) {
                                val beforeFaces = faces.size
                                edgeInsert(ne0, ne1)
                                val afterFaces = faces.size
                                if (afterFaces > beforeFaces) {
                                    activeFaceIds.add(afterFaces - 1)
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    return activeFaceIds
}
