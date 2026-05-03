package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEdgesForVertex {
    @Test
    fun testEdgesForVertex() {
        val vd = VertexData(
            positions = listOf(
                Vector3(0.0, 0.0, 0.0),
                Vector3(1.0, 0.0, 0.0),
                Vector3(1.0, 1.0, 0.0),
                Vector3(0.0, 1.0, 0.0)
            )
        )
        // Two triangles sharing edge (1, 3): (0, 1, 3) and (1, 2, 3)
        // Vertex 1: edges starting at 1 are (1 -> 3) and (1 -> 2)
        // Vertex 3: edges starting at 3 are (3 -> 0) and (3 -> 1)
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

        val edges1 = dcel.edgesForVertex(1)
        assertEquals(2, edges1.size)
        assertTrue(edges1.all { dcel.halfEdges[it].vertex == 1 })

        val edges3 = dcel.edgesForVertex(3)
        assertEquals(2, edges3.size)
        assertTrue(edges3.all { dcel.halfEdges[it].vertex == 3 })

        val edges0 = dcel.edgesForVertex(0)
        assertEquals(1, edges0.size)
        assertEquals(0, dcel.halfEdges[edges0[0]].vertex)

        val edgesNo = dcel.edgesForVertex(-1) // Should probably handle or just fail gracefully
        assertEquals(0, edgesNo.size)
    }

}
