package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.query.componentsForFaces
import org.openrndr.extra.mesh.dcel.query.edgesForFaces

context(dcel: Dcel)
fun FaceList.distinctEdges(): EdgeList {
    return EdgeList(dcel.edgesForFaces(this))
}

context(dcel: Dcel)
fun FaceList.neighbors(): FaceList {
    val seen = this.toMutableSet()
    val result = mutableListOf<Int>()
    for (i in this) {
        val ns = dcel.faces[i].neighbors()
        for (n in ns) {
            if (!seen.contains(n)) {
                seen.add(n)
                result.add(n)
            }
        }
    }
    return FaceList(result)
}

context(dcel: Dcel)
fun FaceList.components(): List<FaceList> {
    return dcel.componentsForFaces(this).map {
        FaceList(it)
    }
}
context(dcel: Dcel)
fun FaceList.edgeLoop(): EdgeList {
    return EdgeList(dcel.edgesForFaces(this))
}