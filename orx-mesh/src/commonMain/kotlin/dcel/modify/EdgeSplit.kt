package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.DCELAttributes
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex


fun <T> Dcel.interpolate(list: MutableList<T>, a: Int, b: Int, f: (T, T) -> T): Int {
    if (a >= 0 && b >= 0) {
        val nextValue = f(list[a], list[b])
        list.add(nextValue)
        return list.size - 1
    } else if (a >= 0) {
        return a
    } else if (b >= 0) {
        return b
    }
    return -1
}

fun Dcel.edgeSplit(e: HalfEdge): Int {
    val eIdx = halfEdges.indexOf(e)
    if (eIdx == -1) return -1

    val v0Idx = e.vertex
    val v1Idx = halfEdges[e.nextEdge].vertex

    val v0 = vertices[v0Idx]
    val v1 = vertices[v1Idx]

    val newVertexPos = (v0.position + v1.position) * 0.5
    val newVertexIdx = vertices.size
    vertices.add(Vertex(newVertexPos, -1))

    // Helper to split a half-edge
    fun splitSingle(edgeIdx: Int, vNewIdx: Int): Int {
        val edge = halfEdges[edgeIdx]
        val nextIdx = edge.nextEdge

        val newEdgeIdx = halfEdges.size
        // New edge goes from vNew to vEnd
        val newEdge = HalfEdge(
            face = edge.face,
            vertex = vNewIdx,
            nextEdge = nextIdx,
            prevEdge = edgeIdx,
            otherEdge = -1,
            attributes = edge.attributes.copyOf()
        )
        halfEdges.add(newEdge)

        // Old edge now goes from vStart to vNew
        edge.nextEdge = newEdgeIdx
        // edge.vertex remains vStart

        // Update next's prev
        if (nextIdx != -1) {
            halfEdges[nextIdx].prevEdge = newEdgeIdx
        }

        // Interpolate attributes for the new edge if they exist
        // Note: in this DCEL, attributes are per half-edge (starting vertex of the edge)
        // Since the new edge starts at vNew, we should probably add new attribute values to the Dcel's lists and point to them.

        // We need the attributes of the next edge to interpolate
        val nextEdge = halfEdges[nextIdx]

        val newAttrs = IntArray(5)
        newAttrs[DCELAttributes.COLOR.index] = interpolate(colors, edge.attributes[DCELAttributes.COLOR.index], nextEdge.attributes[DCELAttributes.COLOR.index]) { a, b -> (a.toLinear() + b.toLinear()).toLinear() * 0.5 }
        newAttrs[DCELAttributes.TEXTURE_COORDINATE.index] = interpolate(textureCoordinates, edge.attributes[DCELAttributes.TEXTURE_COORDINATE.index], nextEdge.attributes[DCELAttributes.TEXTURE_COORDINATE.index]) { a, b -> (a + b) * 0.5 }
        newAttrs[DCELAttributes.NORMAL.index] = interpolate(normals, edge.attributes[DCELAttributes.NORMAL.index], nextEdge.attributes[DCELAttributes.NORMAL.index]) { a, b -> (a + b).normalized }
        newAttrs[DCELAttributes.TANGENT.index] = interpolate(tangents, edge.attributes[DCELAttributes.TANGENT.index], nextEdge.attributes[DCELAttributes.TANGENT.index]) { a, b -> (a + b).normalized }
        newAttrs[DCELAttributes.BITANGENT.index] = interpolate(bitangents, edge.attributes[DCELAttributes.BITANGENT.index], nextEdge.attributes[DCELAttributes.BITANGENT.index]) { a, b -> (a + b).normalized }

        newEdge.attributes = newAttrs

        return newEdgeIdx
    }

    val oeIdx = e.otherEdge
    val newEIdx = splitSingle(eIdx, newVertexIdx)

    if (oeIdx != -1) {
        val newOeIdx = splitSingle(oeIdx, newVertexIdx)

        // Link other edges
        val e0 = halfEdges[eIdx]
        val oe0 = halfEdges[oeIdx]
        val oe1 = halfEdges[newOeIdx]

        // Original: e0 (v0->v1), oe0 (v1->v0)
        // After splitSingle:
        // e0 (v0->vNew), e1 (vNew->v1)
        // oe0 (v1->vNew), oe1 (vNew->v0)

        e0.otherEdge = newOeIdx
        oe1.otherEdge = eIdx

        val e1 = halfEdges[newEIdx]
        e1.otherEdge = oeIdx
        oe0.otherEdge = newEIdx
    }

    vertices[newVertexIdx].edge = newEIdx

    return newVertexIdx
}