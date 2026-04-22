package org.openrndr.extra.mesh.generate

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

fun gridMesh(bounds: Rectangle, columns: Int, rows: Int): MeshData {
    val positions = mutableListOf<Vector3>()
    val textureCoords = mutableListOf<Vector2>()
    val normals = mutableListOf<Vector3>()
    val polygons = mutableListOf<IndexedPolygon>()

    for (y in 0..rows) {
        val v = y.toDouble() / rows
        val py = bounds.y + v * bounds.height
        for (x in 0..columns) {
            val u = x.toDouble() / columns
            val px = bounds.x + u * bounds.width
            positions.add(Vector3(px, py, 0.0))
            textureCoords.add(Vector2(u, v))
            normals.add(Vector3.UNIT_Z)
        }
    }

    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val i0 = y * (columns + 1) + x
            val i1 = i0 + 1
            val i2 = (y + 1) * (columns + 1) + x + 1
            val i3 = (y + 1) * (columns + 1) + x

            polygons.add(
                IndexedPolygon(
                    positions = listOf(i0, i1, i2, i3),
                    textureCoords = listOf(i0, i1, i2, i3),
                    colors = emptyList(),
                    normals = listOf(i0, i1, i2, i3),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        }
    }

    return MeshData(
        VertexData(
            positions = positions,
            textureCoords = textureCoords,
            normals = normals
        ),
        polygons
    )
}