package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.vertexCount() : Int {
    return vertices.count { it.edge != -1 }
}