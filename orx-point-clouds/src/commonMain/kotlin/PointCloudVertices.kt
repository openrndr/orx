package org.openrndr.extra.pointclouds

import org.openrndr.draw.VertexElementType
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.IntVector2

val pointCloudVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
}

val coloredPointCloudVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
    color(dimensions = 4)
}

fun pointCloudVertexBuffer(
    resolution: IntVector2
) = vertexBuffer(
    pointCloudVertexFormat,
    vertexCount = resolution.x * resolution.y
)

fun coloredPointCloudVertexBuffer(
    resolution: IntVector2
) = vertexBuffer(
    coloredPointCloudVertexFormat,
    vertexCount = resolution.x * resolution.y
)
