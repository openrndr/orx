package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

context(dcel: Dcel)
fun EdgeList.toContour(): ShapeContour {
    val closed = true
    val points = mutableListOf<Vector2>()
    for (edgeId in this) {
        val edge = dcel.halfEdges[edgeId]
        points.add(edge.start.xy)
    }
    return ShapeContour.fromPoints(points, closed)
}