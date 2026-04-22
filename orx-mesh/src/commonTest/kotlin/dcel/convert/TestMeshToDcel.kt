package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestMeshToDcel {
    @Test
    fun testTriangle() {
        val vd = VertexData(
            positions = listOf(
                Vector3(0.0, 0.0, 0.0),
                Vector3(1.0, 0.0, 0.0),
                Vector3(0.0, 1.0, 0.0)
            )
        )
        val poly = IndexedPolygon(
            positions = listOf(0, 1, 2),
            textureCoords = emptyList(),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )
        val mesh = MeshData(vd, listOf(poly))
        val dcel = mesh.toDcel()

        assertEquals(3, dcel.vertices.size)
        assertEquals(1, dcel.faces.size)
        assertEquals(3, dcel.halfEdges.size)

        // Check half-edge loop
        val e0 = dcel.halfEdges[0]
        val e1 = dcel.halfEdges[e0.nextEdge]
        val e2 = dcel.halfEdges[e1.nextEdge]
        assertEquals(0, e2.nextEdge)
        assertEquals(e0.vertex, 0)
        assertEquals(e1.vertex, 1)
        assertEquals(e2.vertex, 2)
    }

    @Test
    fun testTwoTriangles() {
        val vd = VertexData(
            positions = listOf(
                Vector3(0.0, 0.0, 0.0),
                Vector3(1.0, 0.0, 0.0),
                Vector3(1.0, 1.0, 0.0),
                Vector3(0.0, 1.0, 0.0)
            )
        )
        // Two triangles sharing edge (1, 3) -> wait, (0, 1, 3) and (1, 2, 3)
        val poly1 = IndexedPolygon(
            positions = listOf(0, 1, 3),
            textureCoords = emptyList(),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )
        val poly2 = IndexedPolygon(
            positions = listOf(1, 2, 3),
            textureCoords = emptyList(),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )
        val mesh = MeshData(vd, listOf(poly1, poly2))
        val dcel = mesh.toDcel()

        assertEquals(4, dcel.vertices.size)
        assertEquals(2, dcel.faces.size)
        assertEquals(6, dcel.halfEdges.size)

        // Find the shared edge (1, 3) and its counterpart (3, 1)
        var sharedEdgeIdx = -1
        for (i in dcel.halfEdges.indices) {
            val e = dcel.halfEdges[i]
            val nextE = dcel.halfEdges[e.nextEdge]
            if (e.vertex == 1 && nextE.vertex == 3) {
                sharedEdgeIdx = i
            }
        }
        assertNotEquals(-1, sharedEdgeIdx)
        val otherIdx = dcel.halfEdges[sharedEdgeIdx].otherEdge
        assertNotEquals(-1, otherIdx)
        assertEquals(3, dcel.halfEdges[otherIdx].vertex)
        val nextOtherE = dcel.halfEdges[dcel.halfEdges[otherIdx].nextEdge]
        assertEquals(1, nextOtherE.vertex)
    }
}
