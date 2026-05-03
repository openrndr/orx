package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector3
import org.openrndr.shape.Winding

fun Dcel.faceWinding(faceId : Int): Winding {
    return edgeloopWinding(faces[faceId].edge)
}

fun Dcel.edgeloopWinding(edgeId: Int): Winding {
    require(edgeId >= 0 && edgeId < halfEdges.size) { "edgeId must be a valid edge index" }

    val vertices = mutableListOf<Vector3>()
    var currentEdgeId = edgeId
    do {
        val edge = halfEdges[currentEdgeId]
        vertices.add(this.vertices[edge.vertex].position)
        currentEdgeId = edge.nextEdge
        require(currentEdgeId >= 0 && currentEdgeId < halfEdges.size) { "invalid nextEdge index $currentEdgeId" }
    } while (currentEdgeId != edgeId && vertices.size < halfEdges.size)

    require(vertices.size >= 3) { "edge loop length must be at least 3" }

    var sum = Vector3.ZERO
    for (i in vertices.indices) {
        val v0 = vertices[i]
        val v1 = vertices[(i + 1) % vertices.size]
        sum += v0.cross(v1)
    }

    if (sum.length > 1E-6) {
        val normal = sum.normalized
        val d = vertices[0].dot(normal)
        for (i in 1 until vertices.size) {
            require(kotlin.math.abs(vertices[i].dot(normal) - d) < 1E-6) { "edge loop is not planar" }
        }
    }

    // In a right-handed system, if we project to XY plane (z=0), 
    // sum.z will be 2 * signed area.
    // In OPENRNDR's default coordinate system (Y-down), 
    // a positive sum.z corresponds to CLOCKWISE winding.
    return if (sum.z > 0) {
        Winding.CLOCKWISE
    } else {
        Winding.COUNTER_CLOCKWISE
    }
}