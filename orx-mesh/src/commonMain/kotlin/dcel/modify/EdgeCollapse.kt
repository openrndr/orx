package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.DCELAttributes
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge

fun Dcel.edgeCollapse(e: HalfEdge) {
    val eIdx = halfEdges.indexOf(e)
    if (eIdx == -1) return

    val v0Idx = e.vertex
    val v1Idx = halfEdges[e.nextEdge].vertex

    // New position is average of the two
    vertices[v0Idx].position = (vertices[v0Idx].position + vertices[v1Idx].position) * 0.5

    fun interpolateAttrs(attrs0: IntArray, attrs1: IntArray): IntArray {
        val newAttrs = IntArray(5)
        newAttrs[DCELAttributes.COLOR.index] = interpolate(colors, attrs0[DCELAttributes.COLOR.index], attrs1[DCELAttributes.COLOR.index]) { a, b -> (a + b) * 0.5 }
        newAttrs[DCELAttributes.TEXTURE_COORDINATE.index] = interpolate(textureCoordinates, attrs0[DCELAttributes.TEXTURE_COORDINATE.index], attrs1[DCELAttributes.TEXTURE_COORDINATE.index]) { a, b -> (a + b) * 0.5 }
        newAttrs[DCELAttributes.NORMAL.index] = interpolate(normals, attrs0[DCELAttributes.NORMAL.index], attrs1[DCELAttributes.NORMAL.index]) { a, b -> (a + b).normalized }
        newAttrs[DCELAttributes.TANGENT.index] = interpolate(tangents, attrs0[DCELAttributes.TANGENT.index], attrs1[DCELAttributes.TANGENT.index]) { a, b -> (a + b).normalized }
        newAttrs[DCELAttributes.BITANGENT.index] = interpolate(bitangents, attrs0[DCELAttributes.BITANGENT.index], attrs1[DCELAttributes.BITANGENT.index]) { a, b -> (a + b).normalized }
        return newAttrs
    }

    // Capture attributes of v1 from e.otherEdge if it exists
    val v1AttributesBase = if (e.otherEdge != -1) halfEdges[e.otherEdge].attributes.copyOf() else e.attributes.copyOf()
    val v0AttributesBase = e.attributes.copyOf()

    // Find all edges starting at v0 and v1
    val v0Edges = halfEdges.filter { it.vertex == v0Idx && it.face != -1 }
    val v1Edges = halfEdges.filter { it.vertex == v1Idx && it.face != -1 }

    // Update attributes
    for (he in v0Edges) {
        println("[DEBUG_LOG] Interpolating edge starting at v0 (original color idx ${he.attributes[0]}) with v1 attributes (color idx ${v1AttributesBase[0]})")
        he.attributes = interpolateAttrs(he.attributes, v1AttributesBase)
        println("[DEBUG_LOG] Result color idx: ${he.attributes[0]}")
    }
    for (he in v1Edges) {
        he.attributes = interpolateAttrs(he.attributes, v0AttributesBase)
    }

    // All edges starting at v1 now start at v0
    for (he in v1Edges) {
        he.vertex = v0Idx
    }

    // Handle e.face removal if it's a triangle
    fun removeTriangle(edgeIdx: Int) {
        val he = halfEdges[edgeIdx]
        val fIdx = he.face
        if (fIdx == -1) return

        val nextIdx = he.nextEdge
        val prevIdx = he.prevEdge

        // If it's a triangle
        if (halfEdges[nextIdx].nextEdge == edgeIdx) {
            val next = halfEdges[nextIdx]
            val prev = halfEdges[prevIdx]

            // Link otherEdges of the other two sides
            val nextOtherIdx = next.otherEdge
            val prevOtherIdx = prev.otherEdge

            if (nextOtherIdx != -1) {
                halfEdges[nextOtherIdx].otherEdge = prevOtherIdx
            }
            if (prevOtherIdx != -1) {
                halfEdges[prevOtherIdx].otherEdge = nextOtherIdx
            }

            // Mark face and edges as deleted
            faces[fIdx].edge = -1
            he.vertex = -1
            next.vertex = -1
            prev.vertex = -1

            he.face = -1
            next.face = -1
            prev.face = -1

            he.attributes = IntArray(5) { -1 }
            next.attributes = IntArray(5) { -1 }
            prev.attributes = IntArray(5) { -1 }
        }
    }

    val oeIdx = e.otherEdge
    removeTriangle(eIdx)
    if (oeIdx != -1) {
        removeTriangle(oeIdx)
    }

    // Mark v1 as deleted
    vertices[v1Idx].edge = -1

    // Update v0.edge if it was deleted
    if (vertices[v0Idx].edge == -1 || halfEdges[vertices[v0Idx].edge].vertex == -1) {
        vertices[v0Idx].edge = halfEdges.indexOfFirst { it.vertex == v0Idx && it.face != -1 }
    }
}