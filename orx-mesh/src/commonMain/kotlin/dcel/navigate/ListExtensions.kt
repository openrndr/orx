package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.EdgeList
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.VertexList

context(dcel: Dcel)
fun List<Int>.filterEdges(f: (HalfEdge) -> Boolean): List<Int> = filter {
    val edge = dcel.halfEdges[it]
    edge.vertex != -1 && f(edge)
}

context(dcel: Dcel)
fun List<Int>.anyEdges(f: (HalfEdge) -> Boolean) : Boolean = any {
    val edge = dcel.halfEdges[it]
    edge.vertex != -1 && f(edge)
}

context(dcel: Dcel)
fun List<Int>.allEdges(f: (HalfEdge) -> Boolean) : Boolean = all {
    val edge = dcel.halfEdges[it]
    edge.vertex != -1 && f(edge)
}

context(dcel: Dcel)
fun List<Int>.noneEdges(f: (HalfEdge) -> Boolean) : Boolean = none {
    val edge = dcel.halfEdges[it]
    edge.vertex != -1 && f(edge)
}


context(dcel: Dcel)
fun EdgeList.filter(f: (HalfEdge) -> Boolean): EdgeList = EdgeList(filterEdges(f))

context(dcel: Dcel)
fun EdgeList.any(f: (HalfEdge) -> Boolean): Boolean = anyEdges(f)

context(dcel: Dcel)
fun EdgeList.none(f: (HalfEdge) -> Boolean): Boolean = noneEdges(f)

context(dcel: Dcel)
fun EdgeList.all(f: (HalfEdge) -> Boolean): Boolean = allEdges(f)


context(dcel: Dcel)
fun List<Int>.filterVertices(f: (Vertex) -> Boolean): List<Int> = filter {
    val vertex = dcel.vertices[it]
    vertex.edge != -1 && f(vertex)
}

context(dcel: Dcel)
fun List<Int>.anyVertices(f: (Vertex) -> Boolean): Boolean = any {
    val vertex = dcel.vertices[it]
    vertex.edge != -1 && f(vertex)
}

context(dcel: Dcel)
fun List<Int>.allVertices(f: (Vertex) -> Boolean): Boolean = all {
    val vertex = dcel.vertices[it]
    vertex.edge != -1 && f(vertex)
}

context(dcel: Dcel)
fun List<Int>.noneVertices(f: (Vertex) -> Boolean): Boolean = none {
    val vertex = dcel.vertices[it]
    vertex.edge != -1 && f(vertex)
}

context(dcel: Dcel)
fun VertexList.filter(f: (Vertex) -> Boolean): VertexList = VertexList(filterVertices(f))

context(dcel: Dcel)
fun VertexList.any(f: (Vertex) -> Boolean): Boolean = anyVertices(f)

context(dcel: Dcel)
fun VertexList.none(f: (Vertex) -> Boolean): Boolean = noneVertices(f)

context(dcel: Dcel)
fun VertexList.all(f: (Vertex) -> Boolean): Boolean = allVertices(f)


context(dcel: Dcel)
fun FaceList.filter(f: (Face) -> Boolean): FaceList = FaceList(filterFaces(f))

context(dcel: Dcel)
fun FaceList.find(f: (Face) -> Boolean): Face? {
    val element = find { i : Int ->
        val face = dcel.faces[i]
        face.edge != -1 && f(face)
    }
    return if (element != null) {
        dcel.faces[element]
    } else {
        null
    }
}

context(dcel: Dcel)
fun FaceList.any(f: (Face) -> Boolean): Boolean = anyFaces(f)

context(dcel: Dcel)
fun FaceList.none(f: (Face) -> Boolean): Boolean = noneFaces(f)

context(dcel: Dcel)
fun FaceList.all(f: (Face) -> Boolean): Boolean = allFaces(f)

context(dcel: Dcel)
fun List<Int>.filterFaces(f: (Face) -> Boolean): List<Int> = filter {
    val face = dcel.faces[it]
    face.edge != -1 && f(face)
}

context(dcel: Dcel)
fun List<Int>.anyFaces(f: (Face) -> Boolean): Boolean = any {
    val face = dcel.faces[it]
    face.edge != -1 && f(face)
}

context(dcel: Dcel)
fun List<Int>.allFaces(f: (Face) -> Boolean): Boolean = all {
    val face = dcel.faces[it]
    face.edge != -1 && f(face)
}

context(dcel: Dcel)
fun List<Int>.noneFaces(f: (Face) -> Boolean): Boolean = none {
    val face = dcel.faces[it]
    face.edge != -1 && f(face)
}