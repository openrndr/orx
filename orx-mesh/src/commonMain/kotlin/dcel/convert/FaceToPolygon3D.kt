package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.shapes.polygon.Polygon3D

fun Dcel.faceToPolygon3D(faceId: Int): Polygon3D {
    val vertices = verticesForFace(faceId)
    return Polygon3D(vertices.map {
        this.vertices[it].position
    })
}