package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.edgeObjectsForFace
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

fun Dcel.faceToShape(faceId: Int): Shape {

    val face = faces[faceId]
    val outer = edgeObjectsForFace(faceId).map { vertices[it.vertex].position.xy }

    val outerContour = ShapeContour.fromPoints(outer, closed = true)

    val holes = face.holeEdges.map {
        edgeLoopIndices(it).map { vertices[ halfEdges[it].vertex].position.xy }
    }

    val holeContours = holes.map {
        ShapeContour.fromPoints(it, closed = true)
    }

    return Shape(listOf(outerContour) + holeContours)

}