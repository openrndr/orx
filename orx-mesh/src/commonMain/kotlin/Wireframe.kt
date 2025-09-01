package org.openrndr.extra.mesh

import org.openrndr.math.Vector3

/**
 * Generates a wireframe representation of the mesh.
 *
 * This method processes the polygons of the mesh data and extracts the vertex positions
 * for each polygon. The result is a list of lists, where each inner list represents the
 * positions of the vertices forming the edges of a polygon in 3D space.
 *
 * @return A list of lists of [Vector3], where each inner list contains the vertex positions
 *         that form the edges of a polygon, effectively representing the wireframe of the mesh.
 */
fun IMeshData.wireframe(): List<List<Vector3>> {
    return polygons.map { ip -> ip.toPolygon(this.vertexData).positions.toList() }
}

/**
 * Generates a wireframe representation of the compound mesh data.
 *
 * This method aggregates the wireframe representations of all meshes within the compounds
 * of the compound mesh data. Each compound's mesh is processed to extract its wireframe
 * as a list of edges represented by vertex positions in 3D space.
 *
 * @return A list of lists of [Vector3], where each inner list represents the vertex positions
 *         forming the edges of a polygon. This effectively provides the wireframe data
 *         for the entire compound mesh.
 */
fun ICompoundMeshData.wireframe(): List<List<Vector3>> {
    return compounds.values.flatMap { it.wireframe() }
}