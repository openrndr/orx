package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.query.edgePoint

fun Dcel.edgeSplitAt(edgeId: Int, t: Double): Int {
    val point = edgePoint(edgeId, t)

    val colorIdx = point.color?.let {
        if (point.attributes[0] == -1) {
            colors.add(it)
            colors.size - 1
        } else {
            point.attributes[0]
        }
    } ?: -1

    val texCoordIdx = point.textureCoordinate?.let {
        if (point.attributes[1] == -1) {
            textureCoordinates.add(it)
            textureCoordinates.size - 1
        } else {
            point.attributes[1]
        }
    } ?: -1

    val normalIdx = point.normal?.let {
        if (point.attributes[2] == -1) {
            normals.add(it)
            normals.size - 1
        } else {
            point.attributes[2]
        }
    } ?: -1

    val tangentIdx = point.tangent?.let {
        if (point.attributes[3] == -1) {
            tangents.add(it)
            tangents.size - 1
        } else {
            point.attributes[3]
        }
    } ?: -1

    val bitangentIdx = point.bitangent?.let {
        if (point.attributes[4] == -1) {
            bitangents.add(it)
            bitangents.size - 1
        } else {
            point.attributes[4]
        }
    } ?: -1

    val newAttrs = intArrayOf(colorIdx, texCoordIdx, normalIdx, tangentIdx, bitangentIdx)

    val newVertexIdx = vertices.size
    vertices.add(Vertex(point.position, -1))

    fun splitSingle(eIdx: Int, vNewIdx: Int, vNewAttrs: IntArray): Int {
        val edge = halfEdges[eIdx]
        val nextIdx = edge.nextEdge

        val newEdgeIdx = halfEdges.size
        val newEdge = HalfEdge(
            face = edge.face,
            vertex = vNewIdx,
            nextEdge = nextIdx,
            prevEdge = eIdx,
            otherEdge = -1,
            attributes = vNewAttrs.copyOf()
        )
        halfEdges.add(newEdge)

        edge.nextEdge = newEdgeIdx
        if (nextIdx != -1) {
            halfEdges[nextIdx].prevEdge = newEdgeIdx
        }

        return newEdgeIdx
    }

    val edge = halfEdges[edgeId]
    val oeIdx = edge.otherEdge

    val newEIdx = splitSingle(edgeId, newVertexIdx, newAttrs)

    if (oeIdx != -1) {
        val oe = halfEdges[oeIdx]
        val oePoint = edgePoint(oeIdx, 1.0 - t)

        val oeColorIdx = oePoint.color?.let {
            if (oePoint.attributes[0] == -1) {
                colors.add(it)
                colors.size - 1
            } else {
                oePoint.attributes[0]
            }
        } ?: -1

        val oeTexCoordIdx = oePoint.textureCoordinate?.let {
            if (oePoint.attributes[1] == -1) {
                textureCoordinates.add(it)
                textureCoordinates.size - 1
            } else {
                oePoint.attributes[1]
            }
        } ?: -1

        val oeNormalIdx = oePoint.normal?.let {
            if (oePoint.attributes[2] == -1) {
                normals.add(it)
                normals.size - 1
            } else {
                oePoint.attributes[2]
            }
        } ?: -1

        val oeTangentIdx = oePoint.tangent?.let {
            if (oePoint.attributes[3] == -1) {
                tangents.add(it)
                tangents.size - 1
            } else {
                oePoint.attributes[3]
            }
        } ?: -1

        val oeBitangentIdx = oePoint.bitangent?.let {
            if (oePoint.attributes[4] == -1) {
                bitangents.add(it)
                bitangents.size - 1
            } else {
                oePoint.attributes[4]
            }
        } ?: -1

        val oeNewAttrs = intArrayOf(oeColorIdx, oeTexCoordIdx, oeNormalIdx, oeTangentIdx, oeBitangentIdx)

        val newOeIdx = splitSingle(oeIdx, newVertexIdx, oeNewAttrs)

        val e0 = halfEdges[edgeId]
        val e1 = halfEdges[newEIdx]
        val oe0 = halfEdges[oeIdx]
        val oe1 = halfEdges[newOeIdx]

        e0.otherEdge = newOeIdx
        oe1.otherEdge = edgeId

        e1.otherEdge = oeIdx
        oe0.otherEdge = newEIdx
    }

    vertices[newVertexIdx].edge = newEIdx
    return newEIdx
}