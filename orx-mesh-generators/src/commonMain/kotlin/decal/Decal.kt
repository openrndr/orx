package org.openrndr.extra.meshgenerators.decal

import org.openrndr.extra.mesh.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.abs

/**
 * Create a decal mesh
 * @param projectorMatrix
 * @param size
 */
fun IMeshData.decal(
    projectorMatrix: Matrix44,
    size: Vector3
): IVertexData {
    require(isTriangular())

    val projectorMatrixInverse = projectorMatrix.inversed

    val positions = vertexData.positions.slice(polygons.flatMap { it.positions }).map {
        (projectorMatrixInverse * (it.xyz1)).div
    }
    val normals = vertexData.normals.slice(polygons.flatMap { it.normals })
    val textureCoords = vertexData.textureCoords.slice(polygons.flatMap { it.textureCoords })
    val colors = vertexData.colors.slice(polygons.flatMap { it.colors })
    val tangents = vertexData.tangents.slice(polygons.flatMap { it.tangents })
    val bitangents = vertexData.bitangents.slice(polygons.flatMap { it.bitangents })

    var decalVertices: IVertexData = VertexData(positions, textureCoords, colors, normals, tangents, bitangents)

    decalVertices = decalVertices.clipToPlane(size, Vector3(1.0, 0.0, 0.0))
    decalVertices = decalVertices.clipToPlane(size, Vector3(-1.0, 0.0, 0.0))
    decalVertices = decalVertices.clipToPlane(size, Vector3(0.0, 1.0, 0.0))
    decalVertices = decalVertices.clipToPlane(size, Vector3(0.0, -1.0, 0.0))
    decalVertices = decalVertices.clipToPlane(size, Vector3(0.0, 0.0, 1.0))
    decalVertices = decalVertices.clipToPlane(size, Vector3(0.0, 0.0, -1.0))


    val decalMesh = MutableVertexData()
    for (i in decalVertices.positions.indices) {
        val v = decalVertices[i]

        val w = v.copy(
            position = (projectorMatrix * v.position.xyz1).div,
            textureCoord = v.position.xy / size.xy + Vector2(0.5)
        )
        decalMesh.add(w)
    }
    return decalMesh
}

fun IVertexData.clipToPlane(
    size: Vector3,
    plane: Vector3
): IVertexData {
    val outVertices = MutableVertexData()
    val s = 0.5 * abs(size.dot(plane))

    fun clip(
        v0: Point,
        v1: Point, p: Vector3, s: Double
    ): Point {

        val d0 = v0.position.dot(p) - s;
        val d1 = v1.position.dot(p) - s;

        val s0 = d0 / (d0 - d1)

        val v = Point(
            v0.position + (v1.position - v0.position) * s0,
            if (v0.textureCoord != null) {
                v0.textureCoord!! + (v1.textureCoord!! - v0.textureCoord!!) * s0
            } else {
                null
            },
            if (v0.color != null) {
                v0.color!! + (v1.color!! - v0.color!!) * s0
            } else {
                null
            },
            if (v0.normal != null) {
                v0.normal!! + (v1.normal!! - v0.normal!!) * s0
            } else {
                null
            },
            if (v0.tangent != null) {
                v0.tangent!! + (v1.tangent!! - v0.tangent!!) * s0
            } else {
                null
            },
            if (v0.bitangent != null) {
                v0.bitangent!! + (v1.bitangent!! - v0.bitangent!!) * s0
            } else {
                null
            }
        )
        return v
    }

    for (i in positions.indices step 3) {

        val d1 = positions[i + 0].dot(plane) - s
        val d2 = positions[i + 1].dot(plane) - s
        val d3 = positions[i + 2].dot(plane) - s

        val v1Out = d1 > 0
        val v2Out = d2 > 0
        val v3Out = d3 > 0

        val total = (if (v1Out) 1 else 0) + (if (v2Out) 1 else 0) + (if (v3Out) 1 else 0)

        when (total) {
            0 -> {
                outVertices.add(this[i])
                outVertices.add(this[i + 1])
                outVertices.add(this[i + 2])
            }

            1 -> {
                if (v1Out) {
                    val nV1 = this[i + 1]
                    val nV2 = this[i + 2]
                    val nV3 = clip(this[i], nV1, plane, s)
                    val nV4 = clip(this[i], nV2, plane, s)

                    outVertices.add(nV1)
                    outVertices.add(nV2)
                    outVertices.add(nV3)

                    outVertices.add(nV4)
                    outVertices.add(nV3)
                    outVertices.add(nV2)
                }

                if (v2Out) {
                    val nV1 = this[i];
                    val nV2 = this[i + 2];
                    val nV3 = clip(this[i + 1], nV1, plane, s)
                    val nV4 = clip(this[i + 1], nV2, plane, s)

                    outVertices.add(nV3)
                    outVertices.add(nV2)
                    outVertices.add(nV1)

                    outVertices.add(nV2)
                    outVertices.add(nV3)
                    outVertices.add(nV4)
                }

                if (v3Out) {
                    val nV1 = this[i]
                    val nV2 = this[i + 1]
                    val nV3 = clip(this[i + 2], nV1, plane, s)
                    val nV4 = clip(this[i + 2], nV2, plane, s)

                    outVertices.add(nV1)
                    outVertices.add(nV2)
                    outVertices.add(nV3)

                    outVertices.add(nV4)
                    outVertices.add(nV3)
                    outVertices.add(nV2)
                }
            }

            2 -> {
                if (!v1Out) {
                    val nV1 = this[i]
                    val nV2 = clip(nV1, this[i + 1], plane, s)
                    val nV3 = clip(nV1, this[i + 2], plane, s)
                    outVertices.add(nV1)
                    outVertices.add(nV2)
                    outVertices.add(nV3)
                }

                if (!v2Out) {
                    val nV1 = this[i + 1]
                    val nV2 = clip(nV1, this[i + 2], plane, s)
                    val nV3 = clip(nV1, this[i], plane, s)
                    outVertices.add(nV1)
                    outVertices.add(nV2)
                    outVertices.add(nV3)
                }

                if (!v3Out) {
                    val nV1 = this[i + 2]
                    val nV2 = clip(nV1, this[i], plane, s)
                    val nV3 = clip(nV1, this[i + 1], plane, s)
                    outVertices.add(nV1)
                    outVertices.add(nV2)
                    outVertices.add(nV3)
                }
            }
            else -> {
            }
        }
    }
    return outVertices
}