package org.openrndr.extra.mesh

import org.openrndr.math.Vector3

/**
 * Extract wireframe from mesh data
 */
fun IMeshData.wireframe(): List<List<Vector3>> {
    return polygons.map { ip -> ip.toPolygon(this.vertexData).positions.toList() }
}

/**
 * Extract wireframe from compound mesh data
 */
fun ICompoundMeshData.wireframe(): List<List<Vector3>> {
    return compounds.values.flatMap { it.wireframe() }
}