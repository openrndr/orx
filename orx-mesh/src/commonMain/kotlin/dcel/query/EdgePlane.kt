package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.shapes.primitives.Plane
import org.openrndr.math.Vector3

fun Dcel.edgePlane(edgeId: Int): Plane {
    val e0 = halfEdges[edgeId]
    val v0 = vertices[e0.vertex].position
    val v1 = vertices[halfEdges[e0.nextEdge].vertex].position
    val faceNormal = faceNormal(e0.face)

    val edgeDirection = (v1 - v0).normalized
    val planeNormal = edgeDirection.cross(faceNormal).normalized

    return Plane(planeNormal, planeNormal.dot(v0))
}