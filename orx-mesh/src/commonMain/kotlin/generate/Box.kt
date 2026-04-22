package org.openrndr.extra.mesh.generate

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

fun boxMesh(center: Vector3, width: Double, height: Double, depth: Double): MeshData {
    val x0 = center.x - width / 2.0
    val x1 = center.x + width / 2.0
    val y0 = center.y - height / 2.0
    val y1 = center.y + height / 2.0
    val z0 = center.z - depth / 2.0
    val z1 = center.z + depth / 2.0

    val positions = listOf(
        Vector3(x0, y0, z0), // 0
        Vector3(x1, y0, z0), // 1
        Vector3(x1, y1, z0), // 2
        Vector3(x0, y1, z0), // 3
        Vector3(x0, y0, z1), // 4
        Vector3(x1, y0, z1), // 5
        Vector3(x1, y1, z1), // 6
        Vector3(x0, y1, z1)  // 7
    )

    val normals = mutableListOf<Vector3>()
    val textureCoords = mutableListOf<Vector2>()
    val polygons = mutableListOf<IndexedPolygon>()

    fun addFace(i0: Int, i1: Int, i2: Int, i3: Int, normal: Vector3) {
        val startIdx = normals.size
        normals.addAll(listOf(normal, normal, normal, normal))
        textureCoords.addAll(
            listOf(
                Vector2(0.0, 0.0),
                Vector2(1.0, 0.0),
                Vector2(1.0, 1.0),
                Vector2(0.0, 1.0)
            )
        )
        polygons.add(
            IndexedPolygon(
                positions = listOf(i0, i1, i2, i3),
                textureCoords = listOf(startIdx, startIdx + 1, startIdx + 2, startIdx + 3),
                colors = emptyList(),
                normals = listOf(startIdx, startIdx + 1, startIdx + 2, startIdx + 3),
                tangents = emptyList(),
                bitangents = emptyList()
            )
        )
    }

    // Front face (z+)
    addFace(4, 5, 6, 7, Vector3(0.0, 0.0, 1.0))

    // Back face (z-)
    addFace(1, 0, 3, 2, Vector3(0.0, 0.0, -1.0))

    // Left face (x-)
    addFace(0, 4, 7, 3, Vector3(-1.0, 0.0, 0.0))

    // Right face (x+)
    addFace(5, 1, 2, 6, Vector3(1.0, 0.0, 0.0))

    // Top face (y+)
    addFace(7, 6, 2, 3, Vector3(0.0, 1.0, 0.0))

    // Bottom face (y-)
    addFace(0, 1, 5, 4, Vector3(0.0, -1.0, 0.0))

    return MeshData(
        VertexData(
            positions = positions,
            normals = normals,
            textureCoords = textureCoords
        ),
        polygons
    )
}