package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter

context(dcel: Dcel)
fun Face.center() {
    val edge = dcel.halfEdges[edge]
    dcel.convexFaceCenter(edge.face)
}