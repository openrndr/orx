package org.openrndr.extra.mesh

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer


/**
 * Converts the compound mesh data into a single [VertexBuffer] for rendering.
 *
 * The method internally triangulates the compound mesh data, calculates the total number
 * of triangles contained within the mesh data, and populates a [VertexBuffer] with
 * the vertex attributes (e.g., positions, normals, texture coordinates, etc.) required
 * for rendering the mesh.
 *
 * @return a [VertexBuffer] containing the vertex data of the triangulated mesh.
 */
fun ICompoundMeshData.toVertexBuffer(): VertexBuffer {
    val triangulated = this.triangulate()

    val triangleCount = triangulated.compounds.values.sumOf { it.polygons.size }

    val vertexBuffer = vertexBuffer(objVertexFormat, triangleCount * 3)

    var elementOffset = 0
    for (compound in compounds) {
        compound.value.toVertexBuffer(elementOffset, vertexBuffer)
        elementOffset += compound.value.polygons.size * 3
    }

    return vertexBuffer
}


/**
 * Converts the compound mesh data into a map of polygons grouped by their compound names.
 *
 * This function traverses the `compounds` map within the `ICompoundMeshData` instance,
 * converting each `IMeshData` to a list of `IPolygon` objects. The resulting map preserves
 * the structure of the original `compounds` map, associating each compound name with its
 * corresponding list of polygons.
 *
 * @return a map where each key is the name of a compound, and the value is a list of [IPolygon]
 *         representing the polygons of the corresponding compound data.
 */
fun ICompoundMeshData.toPolygons(): Map<String, List<IPolygon>> {
    return compounds.mapValues { it.value.toPolygons() }
}