package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.faceCount() : Int {
    return faces.count { it.edge != -1 }
}

fun Dcel.holeCount() : Int {
    return faces.filter { it.edge != -1 }.sumOf { it.holeEdges.size }
}
