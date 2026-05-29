package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.shapes.polygon.Polygon3D

fun Dcel.vertexListToPolygon3D(vertexIds: List<Int>): Polygon3D {
    return Polygon3D(vertexIds.map {
        this.vertices[it].position
    })
}