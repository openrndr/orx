package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.DCELAttributes
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Point
import org.openrndr.math.Vector3

fun Dcel.edgePoint(edgeId: Int, t: Double): Point {
    val e0 = halfEdges[edgeId]
    val e1 = halfEdges[e0.nextEdge]

    val p0 = vertices[e0.vertex].position
    val p1 = vertices[e1.vertex].position

    val resultAttributes = IntArray(e0.attributes.size) { -1 }

    fun <T> interpolate(
        list: List<T>,
        attribute: DCELAttributes,
        mix: (T, T, Double) -> T
    ): T? {
        val idx0 = e0.attributes.getOrNull(attribute.index) ?: -1
        val idx1 = e1.attributes.getOrNull(attribute.index) ?: -1

        return if (idx0 != -1 && idx1 != -1) {
            if (idx0 == idx1) {
                resultAttributes[attribute.index] = idx0
                list[idx0]
            } else {
                mix(list[idx0], list[idx1], t)
            }
        } else if (idx0 != -1) {
            list[idx0]
        } else if (idx1 != -1) {
            list[idx1]
        } else {
            null
        }
    }

    val color = interpolate(colors, DCELAttributes.COLOR) { a, b, t -> (a.toLinear() * (1.0 - t) + b.toLinear() * t).toLinear() }
    val texCoord = interpolate(textureCoordinates, DCELAttributes.TEXTURE_COORDINATE) { a, b, t -> a * (1.0 - t) + b * t }
    val normal = interpolate(normals, DCELAttributes.NORMAL) { a, b, t -> a * (1.0 - t) + b * t }
    val tangent = interpolate(tangents, DCELAttributes.TANGENT) { a, b, t -> a * (1.0 - t) + b * t }
    val bitangent = interpolate(bitangents, DCELAttributes.BITANGENT) { a, b, t -> a * (1.0 - t) + b * t }

    return Point(
        p0 * (1.0 - t) + p1 * t,
        textureCoordinate = texCoord,
        color = color,
        normal = normal,
        tangent = tangent,
        bitangent = bitangent,
        attributes = resultAttributes
    )
}
fun Dcel.edgePosition(edgeId: Int, t: Double): Vector3 {

    val e0 = halfEdges[edgeId]
    val e1 = halfEdges[e0.nextEdge]

    val p0 = vertices[e0.vertex].position
    val p1 = vertices[e1.vertex].position

    return p0 * (1.0 - t) + p1 * t
}

fun Dcel.edgeNearestPosition(edgeId: Int, position: Vector3): Double {
    val e0 = halfEdges[edgeId]
    val e1 = halfEdges[e0.nextEdge]

    val p0 = vertices[e0.vertex].position
    val p1 = vertices[e1.vertex].position

    val v = p1 - p0
    val w = position - p0

    val c1 = w.dot(v)
    if (c1 <= 0.0) return 0.0

    val c2 = v.dot(v)
    if (c2 <= c1) return 1.0

    return c1 / c2
}