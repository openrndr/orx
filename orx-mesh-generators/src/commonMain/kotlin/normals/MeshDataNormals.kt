package org.openrndr.extra.meshgenerators.normals

import org.openrndr.extra.mesh.IMeshData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.math.Vector3

/**
 * Estimate per-vertex normals
 */
fun IMeshData.estimateNormals(): MeshData {
    val normals = MutableList(vertexData.positions.size) { Vector3.ZERO }

    for (polygon in polygons) {
        for (p in polygon.positions) {
            normals[p] += polygon.normal(vertexData)
        }
    }

    for (i in normals.indices) {
        normals[i] = normals[i].normalized
    }

    return MeshData(
        VertexData(
            vertexData.positions,
            vertexData.textureCoords,
            vertexData.colors,
            normals,
            vertexData.tangents,
            vertexData.bitangents
        ),
        polygons.map {
            IndexedPolygon(it.positions, it.textureCoords, it.colors, it.positions, it.tangents, it.bitangents)
        })
}

/**
 * Assign vertex normals based on face normals
 */
fun IMeshData.assignFaceNormals(): MeshData {
    val normals = MutableList(polygons.size) { Vector3.ZERO }

    for (i in polygons.indices) {
        normals[i] = polygons[i].normal(vertexData)
    }

    return MeshData(
        VertexData(
            vertexData.positions,
            vertexData.textureCoords,
            vertexData.colors,
            normals,
            vertexData.tangents,
            vertexData.bitangents
        ),
        polygons.mapIndexed { index, it ->
            IndexedPolygon(it.positions, it.textureCoords, it.colors, it.positions.map {
                index
            }, it.tangents, it.bitangents)
        })
}