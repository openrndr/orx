package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Converts the vertex data stored in the [IVertexData] instance to a [VertexBuffer].
 * The method iterates through the vertex attributes such as positions, normals, texture coordinates,
 * and colors, and writes them sequentially into the provided or newly created [VertexBuffer].
 * Missing attributes are substituted with default values.
 *
 * @param elementOffset The index offset where the vertex data should start being written in the buffer.
 *                       Defaults to 0 if no offset is specified.
 * @param vertexBuffer An optional existing [VertexBuffer] instance to be reused. If null, a new buffer
 *                     will be created.
 * @return A [VertexBuffer] containing the processed vertex data.
 */
fun IVertexData.toVertexBuffer(elementOffset: Int = 0, vertexBuffer: VertexBuffer? = null): VertexBuffer {

    val triangleCount = positions.size / 3
    val vertexBuffer = vertexBuffer ?: vertexBuffer(objVertexFormat, triangleCount * 3)

    vertexBuffer.put(elementOffset) {
        var offset = 0
        for (triangle in 0 until triangleCount) {
            for (i in 0 until 3) {
                write(positions[offset])
                if (normals.isNotEmpty()) {
                    write(normals[offset])
                } else {
                    write(Vector3.ZERO)
                }
                if (textureCoords.isNotEmpty()) {
                    write(textureCoords[offset])
                } else {
                    write(Vector2.ZERO)
                }
                if (colors.isNotEmpty()) {
                    write(colors[offset])
                } else {
                    write(ColorRGBa.WHITE)
                }
                offset++
            }
        }
    }
    vertexBuffer.shadow.destroy()
    return vertexBuffer
}

/**
 * Converts the vertex data into a MeshData representation by constructing the indexed polygons
 * based on the vertex data attributes such as positions, texture coordinates, colors, normals,
 * tangents, and bitangents.
 *
 * Each triangle in the vertex data is represented as an IndexedPolygon, where the indices are
 * generated based on the triangle order, and optional attributes like texture coordinates or
 * colors are included if present in the source vertex data.
 *
 * @return A MeshData instance containing the vertex data and a list of indexed polygons representing the mesh.
 */

fun VertexData.toMeshData(): MeshData {
    val polygons = mutableListOf<IndexedPolygon>()

    val triangleCount = positions.size / 3

    for (t in 0 until triangleCount) {
        val indices = listOf(t * 3, t * 3 + 1, t * 3 + 2)
        polygons.add(
            IndexedPolygon(
                indices,
                if (textureCoords.isNotEmpty()) indices else emptyList(),
                if (colors.isNotEmpty()) indices else emptyList(),
                if (normals.isNotEmpty()) indices else emptyList(),
                if (tangents.isNotEmpty()) indices else emptyList(),
                if (bitangents.isNotEmpty()) indices else emptyList()
            )
        )
    }
    return MeshData(this, polygons)
}