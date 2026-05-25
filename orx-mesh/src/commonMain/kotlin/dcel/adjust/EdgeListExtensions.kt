package org.openrndr.extra.mesh.dcel.adjust

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.modify.edgeSetOffset
import org.openrndr.extra.mesh.dcel.navigate.length
import org.openrndr.extra.mesh.dcel.query.isEdgeloop

context(dcel: Dcel)
fun EdgeList.offset(distance: Double, useJoins: Boolean = false): FaceList {
    return FaceList(dcel.edgeSetOffset(this, distance, useJoins))
}

context(dcel: Dcel)
fun EdgeList.length(): Double {
    var sum = 0.0
    for (i in this) {
        sum += dcel.halfEdges[i].length
    }
    return sum
}

context(dcel: Dcel)
fun EdgeList.isEdgeloop(): Boolean {
    return dcel.isEdgeloop(this)
}