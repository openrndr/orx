package org.openrndr.extra.mesh.dcel.adjust

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.modify.convexFaceSetSubdivide
import org.openrndr.extra.mesh.dcel.modify.faceQuadToTriangle
import org.openrndr.extra.mesh.dcel.modify.faceSetJoin
import org.openrndr.extra.mesh.dcel.modify.faceSetRemove
import org.openrndr.extra.mesh.dcel.modify.faceSetSplit
import org.openrndr.extra.mesh.dcel.modify.vertexSetRelax
import org.openrndr.extra.mesh.dcel.navigate.edgeloop
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.shapes.primitives.Plane

context(dcel: Dcel)
fun FaceList.subdivide(): FaceList {
    return FaceList(dcel.convexFaceSetSubdivide(toSet()).toList())
}

context(dcel: Dcel)
fun FaceList.split(plane: Plane, splitEpsilon: Double = 1E-6): FaceList {
    return FaceList(dcel.faceSetSplit(toSet(), plane, splitEpsilon).toList())
}

context(dcel: Dcel)
fun FaceList.remove(): FaceList {
    dcel.faceSetRemove(toSet())
    return FaceList(emptyList())
}

context(dcel: Dcel)
fun FaceList.join(): FaceList {
    return FaceList(dcel.faceSetJoin(toSet()).toList())
}

context(dcel: Dcel)
fun FaceList.relax() {
    val vertices = mutableSetOf<Int>()
    for (f in this) {
        vertices.addAll(dcel.verticesForFace(f))
    }
    dcel.vertexSetRelax(vertices)
}

context(dcel: Dcel)
fun FaceList.convertQuadsToTriangles(): FaceList {
    val result = mutableListOf<Int>()
    for (f in this) {
        result.addAll(dcel.faceQuadToTriangle(f))
    }
    return FaceList(result)
}