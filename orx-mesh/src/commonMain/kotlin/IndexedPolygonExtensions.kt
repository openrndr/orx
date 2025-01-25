package org.openrndr.extra.mesh

import org.openrndr.math.LinearType
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.max
import kotlin.math.min

internal fun <T : LinearType<T>> bc(barycentric: Vector3, items: List<T>): T {
    return (items[0] * barycentric.x) + (items[1] * barycentric.y) + (items[2] * barycentric.z)
}


/**
 * Computes a `Point` by interpolating vertex attributes from a 3D polygon using
 * barycentric coordinates.
 *
 * @param vertexData The vertex data containing positions, texture coordinates, colors, normals, tangents, and bitangents.
 * @param barycentric The barycentric coordinates used to interpolate the vertex attributes.
 * @return A `Point` containing interpolated vertex attributes including position, texture coordinates, color, normal, tangent, and bitangent.
 */
fun IIndexedPolygon.point(vertexData: IVertexData, barycentric: Vector3): Point {
    require(positions.size == 3)

    val positions = vertexData.positions.slice(positions)
    val colors = vertexData.colors.slice(colors)
    val normals = vertexData.normals.slice(normals)
    val tangents = vertexData.tangents.slice(tangents)
    val bitangents = vertexData.bitangents.slice(bitangents)
    val textureCoords = vertexData.textureCoords.slice(textureCoords)

    return Point(
        (if (positions.isNotEmpty()) bc(barycentric, positions) else null)!!,
        if (textureCoords.isNotEmpty()) bc(barycentric, textureCoords) else null,
        if (colors.isNotEmpty()) bc(barycentric, colors) else null,
        if (normals.isNotEmpty()) bc(barycentric, normals) else null,
        if (tangents.isNotEmpty()) bc(barycentric, tangents) else null,
        if (bitangents.isNotEmpty()) bc(barycentric, bitangents) else null
    )
}


/**
 * Calculates the bounding box of a list of indexed polygons using the specified vertex data.
 *
 * @param vertexData The vertex data containing the positions of the vertices referenced by the polygons.
 * @return A [Box] representing the axis-aligned bounding box of the polygons. If the list is empty, returns an empty box.
 */
fun List<IIndexedPolygon>.bounds(vertexData: IVertexData): Box {
    if (isEmpty()) {
        return Box.EMPTY
    } else {
        var px = Double.NEGATIVE_INFINITY
        var py = Double.NEGATIVE_INFINITY
        var pz = Double.NEGATIVE_INFINITY
        var nx = Double.POSITIVE_INFINITY
        var ny = Double.POSITIVE_INFINITY
        var nz = Double.POSITIVE_INFINITY

        for (p in this) {
            for (i in p.positions) {
                val v = vertexData.positions[i]
                px = max(px, v.x)
                py = max(py, v.y)
                pz = max(pz, v.z)
                nx = min(nx, v.x)
                ny = min(ny, v.y)
                nz = min(nz, v.z)
            }
        }
        return Box(nx, ny, nz, px - nx, py - ny, pz - nz)
    }
}