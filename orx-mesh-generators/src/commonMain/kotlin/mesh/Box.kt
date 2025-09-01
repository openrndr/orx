package org.openrndr.extra.mesh.generators

import org.openrndr.extra.mesh.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class MeshBuilder {
    val vertexData = MutableVertexData()
    val polygons = mutableListOf<IndexedPolygon>()
    val mesh = MutableMeshData(vertexData, polygons)
}

fun box(): MeshData {

    val positions = listOf(
        Vector3(-0.5, -0.5, -0.5),
        Vector3(0.5, -0.5, -0.5),
        Vector3(-0.5, 0.5, -0.5),
        Vector3(0.5, 0.5, -0.5),

        Vector3(-0.5, -0.5, 0.5),
        Vector3(0.5, -0.5, 0.5),
        Vector3(-0.5, 0.5, 0.5),
        Vector3(0.5, 0.5, 0.5),
    )

    val textureCoords = listOf(
        Vector2(0.0, 0.0),
        Vector2(1.0, 0.0),
        Vector2(0.0, 1.0),
        Vector2(1.0, 1.0),
    )

    val normals = listOf(
        Vector3(-1.0, 0.0, 0.0),
        Vector3(1.0, 0.0, 0.0),
        Vector3(0.0, -1.0, 0.0),
        Vector3(0.0, 1.0, 0.0),
        Vector3(0.0, 0.0, -1.0),
        Vector3(0.0, 0.0, 1.0)
    )

    val polygons = listOf(
        // -x
        IndexedPolygon(
            positions = listOf(0, 2, 4, 6),
            textureCoords = listOf(0, 1, 3, 2),
            colors = emptyList(),
            normals = listOf(0, 0, 0, 0),
            tangents = listOf(5, 5, 5, 5),
            bitangents = listOf(3, 3, 3, 3)
        ),
        // +x
        IndexedPolygon(
            positions = listOf(1, 3, 5, 7),
            textureCoords = listOf(0, 1, 3, 2),
            colors = emptyList(),
            normals = listOf(1, 1, 1, 1),
            tangents = listOf(4, 4, 4, 4),
            bitangents = listOf(3, 3, 3, 3)
        )
    )
    return MeshData(
        VertexData(
            positions = positions,
            textureCoords = textureCoords,
            normals = normals,
            tangents = normals,
            bitangents = normals
        ), polygons
    )
}