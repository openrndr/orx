package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.query.convexFaceCenter
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.faceAdjacent
import org.openrndr.extra.mesh.dcel.query.faceContains
import org.openrndr.extra.mesh.dcel.query.isFaceConvex
import org.openrndr.math.Vector3
import org.openrndr.shape.Shape

context(dcel: Dcel)
val Face.id: Int
    get() {
        val edge = dcel.halfEdges[edge]
        return edge.face
    }

context(dcel: Dcel)
fun Face.center(): Vector3 {
    return dcel.convexFaceCenter(id)
}

context(dcel: Dcel)
fun Face.isConvex(): Boolean {
    return dcel.isFaceConvex(id)
}

context(dcel: Dcel)
fun Face.edgeloop(): EdgeList {
    return EdgeList(dcel.edgesForFace(id))
}

context(dcel: Dcel)
fun Face.neighbors(): FaceList = FaceList(dcel.faceAdjacent(id))

context(dcel: Dcel)
fun Face.edgeCount(): Int = edgeloop().size

context(dcel: Dcel)
fun Face.isOnBoundary(): Boolean = edgeloop().anyEdges { it.isBoundary }

context(dcel: Dcel)
fun Face.contains(point: Vector3): Boolean {
    return dcel.faceContains(id, point)
}

context(dcel: Dcel)
fun Face.toShape(): Shape {
    return dcel.faceToShape(id)
}