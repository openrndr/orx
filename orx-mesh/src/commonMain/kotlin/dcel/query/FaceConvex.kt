package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.shapes.polygon.isConvexPolygon

fun Dcel.isFaceConvex(faceId: Int): Boolean {
    val face = faces.getOrNull(faceId) ?: return false
    if (face.holeEdges.isNotEmpty()) return false

    val edges = edgeObjectsForFace(faceId)
    if (edges.size < 3) return true

    val vPositions = edges.map { vertices[it.vertex].position }
    return isConvexPolygon(vPositions)
}