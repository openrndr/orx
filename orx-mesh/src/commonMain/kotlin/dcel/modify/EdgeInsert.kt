package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.math.Vector3

fun isPointInPolygon(p: Vector3, polygon: List<Vector3>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if (((polygon[i].y > p.y) != (polygon[j].y > p.y)) &&
            (p.x < (polygon[j].x - polygon[i].x) * (p.y - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

fun Dcel.edgeInsert(start: Int, end: Int) {
    // insert a new halfEdge
    // the start edge and the end edge are part of the same face
    // the start edge and the end edge are not each other's neighbors
    // inserting the edge will split the face

    val eStart = halfEdges[start]
    val eEnd = halfEdges[end]

    if (eStart.face != eEnd.face || eStart.face == -1) {
        return
    }

    if (eStart.nextEdge == end || eEnd.nextEdge == start) {
        // They are neighbors, cannot split with a new edge (or it would be a degenerate edge)
        return
    }

    val faceIdx = eStart.face
    
    // Check if start and end are on the same loop
    var onSameLoop = false
    var currL = start
    var countL = 0
    while (countL < 1000) {
        if (currL == end) {
            onSameLoop = true
            break
        }
        currL = halfEdges[currL].nextEdge
        if (currL == start || currL == -1) break
        countL++
    }

    if (onSameLoop) {
        val newFaceIdx = faces.size
        val eStartPrev = halfEdges[eStart.prevEdge]
        val eEndPrev = halfEdges[eEnd.prevEdge]

        // New half edges
        val he0Idx = halfEdges.size
        val he1Idx = halfEdges.size + 1

        // he0 goes from start.vertex to end.vertex
        // it will be part of the original faceIdx
        val he0 = HalfEdge(
            face = faceIdx,
            vertex = eStart.vertex,
            nextEdge = end,
            prevEdge = eStart.prevEdge,
            otherEdge = he1Idx,
            attributes = eStart.attributes.copyOf()
        )

        // he1 goes from end.vertex to start.vertex
        // it will be part of a new face
        val he1 = HalfEdge(
            face = newFaceIdx,
            vertex = eEnd.vertex,
            nextEdge = start,
            prevEdge = eEnd.prevEdge,
            otherEdge = he0Idx,
            attributes = eEnd.attributes.copyOf()
        )

        halfEdges.add(he0)
        halfEdges.add(he1)

        // Re-link existing edges
        eStartPrev.nextEdge = he0Idx
        eEndPrev.nextEdge = he1Idx
        eStart.prevEdge = he1Idx
        eEnd.prevEdge = he0Idx

        // Create new face
        faces.add(Face(he1Idx))

        // Update faceIdx's edge if it was one of the reassigned ones
        faces[faceIdx].edge = he0Idx

        // Update face property for all edges that now belong to newFaceIdx
        var curr = start
        var count = 0
        while (curr != he1Idx && count < 1000) {
            halfEdges[curr].face = newFaceIdx
            curr = halfEdges[curr].nextEdge
            count++
        }

        // Distribute holes
        val oldFace = faces[faceIdx]
        val newFaceIdxVal = newFaceIdx
        val oldHoles = oldFace.holeEdges.toMutableList()
        val remainingHoles = mutableListOf<Int>()
        val movedHoles = mutableListOf<Int>()
        
        if (oldHoles.isNotEmpty()) {
            val newFaceBoundaryEdges = mutableListOf<Int>()
            var currB = he1Idx
            do {
                newFaceBoundaryEdges.add(currB)
                currB = halfEdges[currB].nextEdge
            } while (currB != he1Idx && currB != -1)
            
            val newFaceBoundaryPositions = newFaceBoundaryEdges.map { vertices[halfEdges[it].vertex].position }
            
            // Project to 2D for point-in-polygon
            // Use the plane of the face. For simplicity, assume XY if nearly planar or just use the first 3 points to find normal.
            val p0 = newFaceBoundaryPositions[0]
            val p1 = newFaceBoundaryPositions[1]
            val p2 = newFaceBoundaryPositions.getOrNull(2) ?: p0
            val v1 = (p1 - p0).normalized
            val v2 = (p2 - p0).normalized
            var normal = v1.cross(v2).normalized
            if (normal.length < 1e-6) normal = Vector3.UNIT_Z
            
            val right = v1
            val up = normal.cross(right).normalized
            
            fun project(p: Vector3) = Vector3((p - p0).dot(right), (p - p0).dot(up), 0.0)
            
            val poly2D = newFaceBoundaryPositions.map { project(it) }
            
            for (holeStartIdx in oldHoles) {
                val holePos = vertices[halfEdges[holeStartIdx].vertex].position
                val holePos2D = project(holePos)
                
                if (isPointInPolygon(holePos2D, poly2D)) {
                    movedHoles.add(holeStartIdx)
                    // Update all edges in this hole to newFaceIdx
                    var currH = holeStartIdx
                    do {
                        halfEdges[currH].face = newFaceIdxVal
                        currH = halfEdges[currH].nextEdge
                    } while (currH != holeStartIdx && currH != -1)
                } else {
                    remainingHoles.add(holeStartIdx)
                }
            }
            oldFace.holeEdges = remainingHoles.toIntArray()
            faces[newFaceIdxVal].holeEdges = movedHoles.toIntArray()
        }
    } else {
        // Bridging two loops (e.g. outer and hole)
        val eStartPrev = halfEdges[eStart.prevEdge]
        val eEndPrev = halfEdges[eEnd.prevEdge]

        val he0Idx = halfEdges.size
        val he1Idx = halfEdges.size + 1

        val he0 = HalfEdge(
            face = faceIdx,
            vertex = eStart.vertex,
            nextEdge = end,
            prevEdge = eStart.prevEdge,
            otherEdge = he1Idx,
            attributes = eStart.attributes.copyOf()
        )

        val he1 = HalfEdge(
            face = faceIdx,
            vertex = eEnd.vertex,
            nextEdge = start,
            prevEdge = eEnd.prevEdge,
            otherEdge = he0Idx,
            attributes = eEnd.attributes.copyOf()
        )

        halfEdges.add(he0)
        halfEdges.add(he1)

        eStartPrev.nextEdge = he0Idx
        eEndPrev.nextEdge = he1Idx
        eStart.prevEdge = he1Idx
        eEnd.prevEdge = he0Idx
        
        // Remove the hole edge from face.holeEdges if it was one
        val face = faces[faceIdx]
        val holeIdx = face.holeEdges.indexOfFirst {
            var currH = it
            var found = false
            do {
                if (currH == end) {
                    found = true
                    break
                }
                currH = halfEdges[currH].nextEdge
            } while (currH != it && currH != -1)
            found
        }
        
        if (holeIdx != -1) {
            val newHoles = face.holeEdges.toMutableList()
            newHoles.removeAt(holeIdx)
            face.holeEdges = newHoles.toIntArray()
        }
        
        // Ensure the face's main edge is still valid
        face.edge = he0Idx
    }
}