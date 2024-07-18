package org.openrndr.extra.wireframes

import org.openrndr.draw.VertexElementType
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.IntVector2

val wireframeVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
}

val coloredWireframeVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
    color(dimensions = 4)
}

fun wireframeVertexBuffer(
    resolution: IntVector2
) = vertexBuffer(
    wireframeVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 8
)

fun coloredWireframeVertexBuffer(
    resolution: IntVector2
) = vertexBuffer(
    coloredWireframeVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 8
)
