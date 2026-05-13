package org.openrndr.extra.mesh.dcel.adjust

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.FaceList
import org.openrndr.extra.mesh.dcel.modify.convexFaceSetSubdivide
import org.openrndr.extra.mesh.dcel.modify.faceSetSplit
import org.openrndr.extra.shapes.primitives.Plane

context(dcel: Dcel)
fun FaceList.subdivide(): FaceList {
//    require(this.all { it.isConvex() })
    return FaceList(dcel.convexFaceSetSubdivide(toSet()).toList())
}

context(dcel: Dcel)
fun FaceList.split(plane: Plane, splitEpsilon: Double = 1E-6): FaceList {
    return FaceList(dcel.faceSetSplit(toSet(), plane, splitEpsilon).toList())
}