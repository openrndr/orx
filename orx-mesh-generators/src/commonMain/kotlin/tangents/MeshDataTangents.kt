package org.openrndr.extra.meshgenerators.tangents

import org.openrndr.extra.mesh.*
import org.openrndr.math.Vector3

/**
 * Estimate tangents from normals and texture coordinates
 * https://terathon.com/blog/tangent-space.html
 */
fun IMeshData.estimateTangents(): MeshData {
    require(vertexData.textureCoords.isNotEmpty()) {
        "need texture coordinates to estimate tangents"
    }
    require(isTriangular()) {

    }

    val normals = MutableList(vertexData.positions.size) { Vector3.ZERO }

    val tan1 = MutableList(vertexData.positions.size) { Vector3.ZERO }
    val tan2 = MutableList(vertexData.positions.size) { Vector3.ZERO }
    for (polygon in polygons) {
        val v1 = vertexData.positions[polygon.positions[0]]
        val v2 = vertexData.positions[polygon.positions[1]]
        val v3 = vertexData.positions[polygon.positions[2]]

        val w1 = vertexData.textureCoords[polygon.textureCoords[0]]
        val w2 = vertexData.textureCoords[polygon.textureCoords[1]]
        val w3 = vertexData.textureCoords[polygon.textureCoords[2]]

        val x1 = (v2.x - v1.x)
        val x2 = (v3.x - v1.x)
        val y1 = (v2.y - v1.y)
        val y2 = (v3.y - v1.y)
        val z1 = (v2.z - v1.z)
        val z2 = (v3.z - v1.z)

        val s1 = (w2.x - w1.x)
        val s2 = (w3.x - w1.x)
        val t1 = (w2.y - w1.y)
        val t2 = (w3.y - w1.y)

        var det = s1 * t2 - s2 * t1
        if (det == 0.0) det = 1.0
        val r = 1.0/ (det)
        val sdir = Vector3(
            (t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r,
            (t2 * z1 - t1 * z2) * r
        ).normalized
        val tdir = Vector3(
            (s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r,
            (s1 * z2 - s2 * z1) * r
        ).normalized

        tan1[polygon.positions[0]] += sdir
        tan1[polygon.positions[1]] += sdir
        tan1[polygon.positions[2]] += sdir

        tan2[polygon.positions[0]] += tdir
        tan2[polygon.positions[1]] += tdir
        tan2[polygon.positions[2]] += tdir

        normals[polygon.positions[0]] += vertexData.normals[polygon.normals[0]]
        normals[polygon.positions[1]] += vertexData.normals[polygon.normals[1]]
        normals[polygon.positions[2]] += vertexData.normals[polygon.normals[2]]

    }

    for (a in 0 until vertexData.positions.size) {
        normals[a] = normals[a].normalized
        tan1[a] = tan1[a].normalized
        tan2[a] = tan2[a].normalized

        val t = tan1[a]
        val n = normals[a]

        tan1[a] = (t - n * n.dot(t)).normalized
        val w = if ((n.cross(t)).dot(tan2[a]) < 0.0f) -1.0 else 1.0
        tan2[a] = n.cross((t)).normalized * w
    }

    return MeshData(VertexData(vertexData.positions, vertexData.textureCoords, vertexData.colors, normals, tan1, tan2),
        polygons = polygons.map {
            IndexedPolygon(
                it.positions,
                it.textureCoords,
                it.colors,
                normals = it.positions,
                tangents = it.positions,
                bitangents = it.positions
            )
        }
    )
}