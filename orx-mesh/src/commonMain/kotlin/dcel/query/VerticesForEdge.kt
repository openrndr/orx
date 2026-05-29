package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel

fun Dcel.verticesForEdge(edge: Int): List<Int> {
    if (edge < 0 || edge >= halfEdges.size) return emptyList()
    val e0 = halfEdges[edge]
    val v0 = e0.vertex
    val v1 = halfEdges[e0.nextEdge].vertex
    return listOf(v0, v1)
}
