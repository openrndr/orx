package org.openrndr.extra.objloader

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2

private val objVertexFormat = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
}

fun MeshData.toVertexBuffer() : VertexBuffer {
    val objects = triangulate().flattenPolygons()
    val triangleCount = objects.values.sumOf { it.size }
    val vertexBuffer = vertexBuffer(objVertexFormat, triangleCount * 3)

    vertexBuffer.put {
        objects.entries.forEach {
            it.value.forEach {
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
                }
            }
        }
    }

    vertexBuffer.shadow.destroy()
    return vertexBuffer
}