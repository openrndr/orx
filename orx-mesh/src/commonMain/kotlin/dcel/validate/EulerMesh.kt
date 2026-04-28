package org.openrndr.extra.mesh.dcel.validate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgeBoundaryCount
import org.openrndr.extra.mesh.dcel.query.wholeEdgeCount
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.faceCount
import org.openrndr.extra.mesh.dcel.query.vertexCount

fun Dcel.isEulerMesh(): Boolean {
    val v = vertexCount()
    val f = faceCount()
    val e = wholeEdgeCount()
    val h = edgeBoundaryCount()

    for (i in faces.indices) {
        val es = edgesForFace(i)
    }

    if (e-v + 2 - h != f) {
        println("e: $e, v: $v, f: $f, h: $h")
        return false
    }
    return true

}