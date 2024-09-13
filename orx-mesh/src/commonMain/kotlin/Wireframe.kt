package org.openrndr.extra.objloader

import org.openrndr.math.Vector3

fun IMeshData.wireframe(): List<List<Vector3>> {
    return polygons.map { ip -> ip.toPolygon(this.vertexData).positions.toList() }
}

fun ICompoundMeshData.wireframe(): List<List<Vector3>> {
    return compounds.values.flatMap {
        it.wireframe()
    }
}