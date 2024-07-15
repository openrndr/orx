package org.openrndr.extra.objloader

import org.openrndr.math.Vector3

fun MeshData.wireframe() : List<List<Vector3>> {
    return polygonGroups.values.flatMap {
        it.map { ip -> ip.toPolygon(this.vertexData).positions.toList() }
    }
}