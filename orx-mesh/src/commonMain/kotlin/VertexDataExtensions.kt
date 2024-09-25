package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Convert vertex data to [VertexBuffer]. Assumes every 3 consecutive vertices encode a triangle.
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
 * Convert vertex data to [MeshData]. Assumes every 3 consecutive vertices encode a triangle.
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