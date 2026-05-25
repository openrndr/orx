package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.*
import org.openrndr.math.Vector2
import kotlin.math.atan2

fun angularSweep(points: List<Vector2>, edges: List<Pair<Int, Int>>): Dcel {
    val dcel = Dcel()

    // 1. Unique vertices
    dcel.vertices.addAll(points.map { Vertex(it.xy0, -1) })

    // Build a map from position to index, handling potential floating point issues if necessary
    // However, the test uses indices directly in edges, so we assume points are already unique and indexed.
    
    // 2. Create half-edges
    // Use a set of pairs to handle duplicated edges and normalize undirected edges
    val uniqueEdges = edges.map { (a, b) -> if (a < b) a to b else b to a }.toSet()

    class TempEdge(val from: Int, val to: Int) {
        var id: Int = -1
        var twinId: Int = -1
        val angle = atan2(points[to].y - points[from].y, points[to].x - points[from].x)
    }

    val tempEdges = mutableListOf<TempEdge>()
    val outgoing = Array(points.size) { mutableListOf<TempEdge>() }

    // Map to keep track of used vertex indices
    val usedVertices = mutableSetOf<Int>()

    for ((u, v) in uniqueEdges) {
        val e1 = TempEdge(u, v)
        val e2 = TempEdge(v, u)
        
        e1.id = tempEdges.size
        tempEdges.add(e1)
        e2.id = tempEdges.size
        tempEdges.add(e2)
        
        e1.twinId = e2.id
        e2.twinId = e1.id
        
        outgoing[u].add(e1)
        outgoing[v].add(e2)
        usedVertices.add(u)
        usedVertices.add(v)
    }

    // Initialize halfEdges in DCEL
    for (i in tempEdges.indices) {
        val te = tempEdges[i]
        dcel.halfEdges.add(HalfEdge(-1, te.to, -1, -1, te.twinId, IntArray(0)))
    }

    // Set vertex.edge to one of its outgoing half-edges
    for (i in usedVertices) {
        val out = outgoing[i]
        if (out.isNotEmpty()) {
            dcel.vertices[i].edge = out[0].id
        }
    }

    // 3. Link half-edges by sorting outgoing edges at each vertex
    for (v in points.indices) {
        val out = outgoing[v]
        if (out.isEmpty()) continue
        
        // Sort outgoing edges counter-clockwise
        out.sortBy { it.angle }
        
        for (i in out.indices) {
            val e_out = out[i]
            val e_in_id = e_out.twinId
            
            // In CCW winding, if we come into V via e_in, we want to pick the outgoing edge 
            // that is the first one we hit when rotating e_in CCW around V.
            // Rotating e_in CCW around V is equivalent to rotating e_out (twin of e_in) CCW around V
            // and picking the one JUST BEFORE it in CCW order.
            
            val prev_idx = if (i == 0) out.size - 1 else i - 1
            val e_out_prev = out[prev_idx]
            
            // e_in.next = e_out_prev
            dcel.halfEdges[e_in_id].nextEdge = e_out_prev.id
            dcel.halfEdges[e_out_prev.id].prevEdge = e_in_id
        }
    }

    // 4. Traverse loops to find faces
    val visited = BooleanArray(dcel.halfEdges.size)
    val foundFaces = mutableListOf<Face>()
    val faceEdgeLoops = mutableListOf<List<Int>>()

    for (i in dcel.halfEdges.indices) {
        if (!visited[i]) {
            val loop = mutableListOf<Int>()
            var curr = i
            while (!visited[curr]) {
                visited[curr] = true
                loop.add(curr)
                curr = dcel.halfEdges[curr].nextEdge
            }
            
            val faceId = foundFaces.size
            foundFaces.add(Face(i))
            faceEdgeLoops.add(loop)
            for (edgeId in loop) {
                dcel.halfEdges[edgeId].face = faceId
            }
        }
    }

    // 5. Filter out the outer face
    val faceAreas = foundFaces.indices.map { faceIdx ->
        val loop = faceEdgeLoops[faceIdx]
        var area = 0.0
        for (edgeId in loop) {
            val v1 = points[tempEdges[edgeId].from]
            val v2 = points[tempEdges[edgeId].to]
            area += (v1.x * v2.y - v2.x * v1.y)
        }
        area
    }
    
    // In OpenRNDR (Y-down), positive area is CLOCKWISE. 
    // Usually inner faces of polygons are CLOCKWISE.
    // The outer face will have negative area (COUNTER-CLOCKWISE).
    
    val innerFaceIndices = faceAreas.indices.filter { faceAreas[it] > 0 }
    
    dcel.faces.clear()
    dcel.faces.addAll(innerFaceIndices.map { foundFaces[it] })
    
    // Re-assign face IDs to half-edges
    for (i in dcel.halfEdges.indices) {
        val oldFaceId = dcel.halfEdges[i].face
        val newFaceId = innerFaceIndices.indexOf(oldFaceId)
        dcel.halfEdges[i].face = newFaceId 
    }

    // Update Face.edge to point to correct half-edge
    for (i in dcel.faces.indices) {
        val f = dcel.faces[i]
        for (j in dcel.halfEdges.indices) {
            if (dcel.halfEdges[j].face == i) {
                f.edge = j
                break
            }
        }
    }

    // Set otherEdge to -1 for half-edges whose twin belongs to the outer face (face = -1)
    for (i in dcel.halfEdges.indices) {
        val twinId = dcel.halfEdges[i].otherEdge
        if (twinId != -1 && dcel.halfEdges[twinId].face == -1) {
            dcel.halfEdges[i].otherEdge = -1
        }
    }

    // Optional: remove half-edges belonging to the outer face
    val halfEdgeMap = mutableMapOf<Int, Int>()
    val newHalfEdges = mutableListOf<HalfEdge>()
    for (i in dcel.halfEdges.indices) {
        if (dcel.halfEdges[i].face != -1) {
            halfEdgeMap[i] = newHalfEdges.size
            newHalfEdges.add(dcel.halfEdges[i])
        }
    }

    dcel.halfEdges.clear()
    dcel.halfEdges.addAll(newHalfEdges)

    // Update indices in half-edges
    for (he in dcel.halfEdges) {
        he.nextEdge = halfEdgeMap[he.nextEdge] ?: -1
        he.prevEdge = halfEdgeMap[he.prevEdge] ?: -1
        he.otherEdge = halfEdgeMap[he.otherEdge] ?: -1
    }

    // Update indices in vertices and faces
    for (i in dcel.vertices.indices) {
        // Find an edge for this vertex that is NOT removed
        val out = outgoing[i]
        var foundEdge = -1
        for (te in out) {
            if (halfEdgeMap.containsKey(te.id)) {
                foundEdge = halfEdgeMap[te.id]!!
                break
            }
        }
        dcel.vertices[i].edge = foundEdge
    }
    for (f in dcel.faces) {
        f.edge = halfEdgeMap[f.edge] ?: -1
    }

    return dcel
}