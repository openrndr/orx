package org.openrndr.extra.objloader

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2

/**
 * The [VertexFormat] for a [VertexBuffer] with positions, normals and texture coordinates.
 */
internal val objVertexFormat = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
    color(4)
}

/**
 * Converts a [MeshData] instance into a [VertexBuffer]
 */
fun IMeshData.toVertexBuffer(elementOffset: Int = 0, vertexBuffer: VertexBuffer? = null): VertexBuffer {
    val objects = triangulate().toPolygons()
    val triangleCount = objects.size
    val vertexBuffer = vertexBuffer ?: vertexBuffer(objVertexFormat, triangleCount * 3)

    vertexBuffer.put(elementOffset) {
        objects.forEach {

            for (i in it.positions.indices) {
                write(it.positions[i])
                if (it.normals.isNotEmpty()) {
                    write(it.normals[i])
                } else {
                    val d0 = it.positions[2] - it.positions[0]
                    val d1 = it.positions[1] - it.positions[0]
                    write(d0.normalized.cross(d1.normalized).normalized)
                }
                if (it.textureCoords.isNotEmpty()) {
                    write(it.textureCoords[i])
                } else {
                    write(Vector2.ZERO)
                }
                if (it.colors.isNotEmpty()) {
                    write(it.colors[i])
                } else {
                    write(ColorRGBa.WHITE)
                }
            }
        }
    }
    vertexBuffer.shadow.destroy()
    return vertexBuffer
}