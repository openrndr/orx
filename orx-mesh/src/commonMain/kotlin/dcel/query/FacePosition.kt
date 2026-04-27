package org.openrndr.extra.mesh.dcel.query

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.meanvaluecoordinates.findMVCWeights
import org.openrndr.extra.mesh.dcel.DCELAttributes
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Point
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

private fun weightedSum(values: List<ColorRGBa>, weights: DoubleArray): ColorRGBa {
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var a = 0.0
    for (i in values.indices) {
        val v = values[i].toLinear()
        val w = weights[i]
        r += v.r * w
        g += v.g * w
        b += v.b * w
        a += v.a * w
    }
    return ColorRGBa(r, g, b, a)
}

private fun weightedSum(values: List<Vector2>, weights: DoubleArray): Vector2 {
    var x = 0.0
    var y = 0.0
    for (i in values.indices) {
        val v = values[i]
        val w = weights[i]
        x += v.x * w
        y += v.y * w
    }
    return Vector2(x, y)
}

private fun weightedSum(values: List<Vector3>, weights: DoubleArray): Vector3 {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    for (i in values.indices) {
        val v = values[i]
        val w = weights[i]
        x += v.x * w
        y += v.y * w
        z += v.z * w
    }
    return Vector3(x, y, z)
}

fun Dcel.facePoint(faceId: Int, position: Vector3): Point {
    val faceObj = faces.getOrNull(faceId) ?: return Point(position)
    val edgeIndices = edgeLoopIndices(faceObj.edge)

    if (edgeIndices.isEmpty()) return Point(position)

    val faceVerticesPositions = edgeIndices.map { vertices[halfEdges[it].vertex].position }

    // Calculate face normal for MVC
    var faceNormal = Vector3.ZERO
    for (i in faceVerticesPositions.indices) {
        val v0 = faceVerticesPositions[i]
        val v1 = faceVerticesPositions[(i + 1) % faceVerticesPositions.size]
        val v2 = faceVerticesPositions[(i + 2) % faceVerticesPositions.size]
        val cross = (v1 - v0).cross(v2 - v1)
        if (cross.length > 1e-6) {
            faceNormal = cross.normalized
            break
        }
    }
    if (faceNormal == Vector3.ZERO) faceNormal = Vector3.UNIT_Z

    val weights = findMVCWeights(faceVerticesPositions, position, faceNormal)

    fun <T> resolveAttributeValue(
        attributeList: List<T>,
        attributeType: DCELAttributes,
        weightedSumFunc: (List<T>, DoubleArray) -> T
    ): T? {
        val indices = edgeIndices.map { halfEdges[it].attributes.getOrNull(attributeType.index) ?: -1 }
        return if (indices.all { it > -1 }) {
            val values = indices.map { attributeList[it] }
            weightedSumFunc(values, weights)
        } else {
            null
        }
    }

    fun resolveAttributeIndex(attributeType: DCELAttributes): Int {
        val indices = edgeIndices.map { halfEdges[it].attributes.getOrNull(attributeType.index) ?: -1 }
        return if (indices.isNotEmpty() && indices.all { it == indices[0] }) {
            indices[0]
        } else {
            -1
        }
    }

    val color = resolveAttributeValue(colors, DCELAttributes.COLOR, ::weightedSum)
    val texCoord = resolveAttributeValue(textureCoordinates, DCELAttributes.TEXTURE_COORDINATE, ::weightedSum)
    val normal = resolveAttributeValue(normals, DCELAttributes.NORMAL, ::weightedSum)
    val tangent = resolveAttributeValue(tangents, DCELAttributes.TANGENT, ::weightedSum)
    val bitangent = resolveAttributeValue(bitangents, DCELAttributes.BITANGENT, ::weightedSum)

    val finalAttributes = IntArray(DCELAttributes.values().size) {
        resolveAttributeIndex(DCELAttributes.values()[it])
    }

    return Point(
        position = position,
        textureCoordinate = texCoord,
        color = color,
        normal = normal,
        tangent = tangent,
        bitangent = bitangent,
        attributes = finalAttributes
    )
}