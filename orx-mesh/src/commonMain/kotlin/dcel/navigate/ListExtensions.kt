package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex

context(dcel: Dcel)
fun List<Int>.filterEdges(f: (HalfEdge) -> Boolean): List<Int> = filter {
    val edge = dcel.halfEdges[it]
    edge.vertex != -1 && f(edge)
}

context(dcel: Dcel)
fun List<Int>.filterVertices(f: (Vertex) -> Boolean): List<Int> = filter {
    val vertex = dcel.vertices[it]
    vertex.edge != -1 && f(vertex)
}

context(dcel: Dcel)
fun List<Int>.filterFaces(f: (Face) -> Boolean): List<Int> = filter {
    val face = dcel.faces[it]
    face.edge != -1 && f(face)
}