package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector3

import kotlin.math.abs

fun Dcel.isEdgeLoopPlanar(startEdge: Int): Boolean {
    val indices = edgeLoopIndices(startEdge)
    if (indices.size < 4) return true

    val positions = indices.map { vertices[halfEdges[it].vertex].position }

    var normal = Vector3.ZERO
    val p0 = positions[0]
    
    // Find a suitable normal
    for (i in 0 until positions.size) {
        val p1 = positions[i]
        val p2 = positions[(i + 1) % positions.size]
        val p3 = positions[(i + 2) % positions.size]
        val v1 = p2 - p1
        val v2 = p3 - p2
        val cross = v1.cross(v2)
        if (cross.length > 1e-6) {
            normal = cross.normalized
            break
        }
    }

    if (normal == Vector3.ZERO) return true // All points are collinear or identical

    for (position in positions) {
        val v = position - p0
        if (abs(v.dot(normal)) > 1e-6) {
            return false
        }
    }

    return true
}