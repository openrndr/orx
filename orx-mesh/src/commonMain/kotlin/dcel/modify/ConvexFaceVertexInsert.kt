package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

fun weightedSum(values: List<ColorRGBa>, weights: DoubleArray): ColorRGBa {
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

fun weightedSum(values: List<Vector2>, weights: DoubleArray): Vector2 {
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

fun weightedSum(values: List<Vector3>, weights: DoubleArray): Vector3 {
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

fun Dcel.convexFaceVertexInsert(
    faceId: Int, position: Vector3,
    color: ColorRGBa? = null, colorIndex: Int? = null,
    textureCoordinate: Vector2? = null, textureCoordinateIndex: Int? = null,
    normal: Vector3? = null, normalIndex: Int? = null,
    tangent: Vector3? = null, tangentIndex: Int? = null,
    bitangent: Vector3? = null, bitangentIndex: Int? = null
) {
    val faceObj = faces.getOrNull(faceId) ?: return
    val startEdgeIdx = faceObj.edge
    if (startEdgeIdx == -1) return

    val edgeIndices = mutableListOf<Int>()
    var currIdx = startEdgeIdx
    do {
        edgeIndices.add(currIdx)
        currIdx = halfEdges[currIdx].nextEdge
    } while (currIdx != startEdgeIdx && currIdx != -1)

    if (edgeIndices.size < 3) return

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

    val weights = org.openrndr.extra.math.meanvaluecoordinates.findMVCWeights(faceVerticesPositions, position, faceNormal)

    fun <T> resolveAttribute(
        providedValue: T?,
        providedIndex: Int?,
        attributeList: MutableList<T>,
        attributeType: org.openrndr.extra.mesh.dcel.DCELAttributes,
        weightedSumFunc: (List<T>, DoubleArray) -> T
    ): Int {
        if (providedIndex != null) return providedIndex
        if (providedValue != null) {
            attributeList.add(providedValue)
            return attributeList.size - 1
        }

        val indices = edgeIndices.map { halfEdges[it].attributes.getOrNull(attributeType.index) ?: -1 }
        if (indices.all { it > -1 }) {
            val values = indices.map { attributeList[it] }
            val interpolated = weightedSumFunc(values, weights)
            attributeList.add(interpolated)
            return attributeList.size - 1
        }
        return -1
    }

    val finalColorIdx = resolveAttribute(color, colorIndex, colors, org.openrndr.extra.mesh.dcel.DCELAttributes.COLOR, ::weightedSum)
    val finalTexCoordIdx = resolveAttribute(textureCoordinate, textureCoordinateIndex, textureCoordinates, org.openrndr.extra.mesh.dcel.DCELAttributes.TEXTURE_COORDINATE, ::weightedSum)
    val finalNormalIdx = resolveAttribute(normal, normalIndex, normals, org.openrndr.extra.mesh.dcel.DCELAttributes.NORMAL, ::weightedSum)
    val finalTangentIdx = resolveAttribute(tangent, tangentIndex, tangents, org.openrndr.extra.mesh.dcel.DCELAttributes.TANGENT, ::weightedSum)
    val finalBitangentIdx = resolveAttribute(bitangent, bitangentIndex, bitangents, org.openrndr.extra.mesh.dcel.DCELAttributes.BITANGENT, ::weightedSum)

    val newVertexIdx = vertices.size
    vertices.add(org.openrndr.extra.mesh.dcel.Vertex(position, -1))

    val n = edgeIndices.size
    val newEdgesFromVertex = IntArray(n) { halfEdges.size + it * 2 }
    val newEdgesToVertex = IntArray(n) { halfEdges.size + it * 2 + 1 }

    // We will reuse the original face for the first triangle
    // and create n-1 new faces for the others.
    val faceIndices = IntArray(n)
    faceIndices[0] = faceId
    for (i in 1 until n) {
        faceIndices[i] = faces.size
        faces.add(org.openrndr.extra.mesh.dcel.Face(-1))
    }

    for (i in 0 until n) {
        val originalEdgeIdx = edgeIndices[i]
        
        val eFrom = newEdgesFromVertex[i]
        val eTo = newEdgesToVertex[i]
        
        val fIdx = faceIndices[i]

        // Update original edge
        val e = halfEdges[originalEdgeIdx]
        e.face = fIdx
        e.nextEdge = eTo
        e.prevEdge = eFrom

        // Create eFrom (vertex -> start of original edge)
        val eFromObj = org.openrndr.extra.mesh.dcel.HalfEdge(
            face = fIdx,
            vertex = newVertexIdx,
            nextEdge = originalEdgeIdx,
            prevEdge = eTo,
            otherEdge = newEdgesToVertex[(i - 1 + n) % n],
            attributes = IntArray(5) { -1 }.apply {
                this[org.openrndr.extra.mesh.dcel.DCELAttributes.COLOR.index] = finalColorIdx
                this[org.openrndr.extra.mesh.dcel.DCELAttributes.TEXTURE_COORDINATE.index] = finalTexCoordIdx
                this[org.openrndr.extra.mesh.dcel.DCELAttributes.NORMAL.index] = finalNormalIdx
                this[org.openrndr.extra.mesh.dcel.DCELAttributes.TANGENT.index] = finalTangentIdx
                this[org.openrndr.extra.mesh.dcel.DCELAttributes.BITANGENT.index] = finalBitangentIdx
            }
        )
        
        // Create eTo (end of original edge -> vertex)
        val nextOriginalEdgeIdx = edgeIndices[(i + 1) % n]
        val eToObj = org.openrndr.extra.mesh.dcel.HalfEdge(
            face = fIdx,
            vertex = halfEdges[nextOriginalEdgeIdx].vertex,
            nextEdge = eFrom,
            prevEdge = originalEdgeIdx,
            otherEdge = newEdgesFromVertex[(i + 1) % n],
            attributes = halfEdges[nextOriginalEdgeIdx].attributes.copyOf()
        )

        halfEdges.add(eFromObj)
        halfEdges.add(eToObj)
        
        faces[fIdx].edge = originalEdgeIdx
    }

    vertices[newVertexIdx].edge = newEdgesFromVertex[0]
}