package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEdgeloopForFaces {
    @Test
    fun testSingleTriangle() {
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
        val dcel = MeshData(vd, listOf(poly)).toDcel()
        
        val loop = dcel.edgeloopForFaces(listOf(0))
        assertEquals(3, loop.size)
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
        // Two triangles forming a square, shared edge (1, 3)
        val poly1 = IndexedPolygon(listOf(0, 1, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val poly2 = IndexedPolygon(listOf(1, 2, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val dcel = MeshData(vd, listOf(poly1, poly2)).toDcel()

        val loop = dcel.edgeloopForFaces(listOf(0, 1))
        // Boundary should be 0-1, 1-2, 2-3, 3-0. (4 edges)
        assertEquals(4, loop.size)
    }

    @Test
    fun testHole() {
        val vd = VertexData(
            positions = listOf(
                Vector3(0.0, 0.0, 0.0), // 0
                Vector3(3.0, 0.0, 0.0), // 1
                Vector3(3.0, 3.0, 0.0), // 2
                Vector3(0.0, 3.0, 0.0), // 3
                Vector3(1.0, 1.0, 0.0), // 4
                Vector3(2.0, 1.0, 0.0), // 5
                Vector3(2.0, 2.0, 0.0), // 6
                Vector3(1.0, 2.0, 0.0)  // 7
            )
        )
        // A ring of 8 triangles
        val polys = listOf(
            IndexedPolygon(listOf(0, 1, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(1, 5, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(1, 2, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(2, 6, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(2, 3, 6), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(3, 7, 6), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(3, 0, 7), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            IndexedPolygon(listOf(0, 4, 7), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )
        val dcel = MeshData(vd, polys).toDcel()
        val loop = dcel.edgeloopForFaces((0 until 8).toList())
        // Outer loop should have 4 edges (0-1, 1-2, 2-3, 3-0)
        // or inner loop should have 4 edges (4-5, 5-6, 6-7, 7-4)
        // Since my implementation picks one boundary edge and follows it, it should return one of them.
        assertEquals(4, loop.size)
    }
}
