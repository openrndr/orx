package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestFaceSetRemove {
    @Test
    fun testRemoveFace() {
        val positions = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(1.0, 0.0, 0.0),
            Vector3(1.0, 1.0, 0.0),
            Vector3(0.0, 1.0, 0.0),
            Vector3(2.0, 0.0, 0.0),
            Vector3(2.0, 1.0, 0.0)
        )
        val polygons = listOf(
            IndexedPolygon(listOf(0, 1, 2, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 0
            IndexedPolygon(listOf(1, 4, 5, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())  // Face 1
        )
        val meshData = MeshData(VertexData(positions = positions), polygons)
        val dcel = meshData.toDcel()

        assertEquals(2, dcel.faces.filter { it.edge != -1 }.size)
        assertEquals(6, dcel.vertices.filter { it.edge != -1 }.size)

        // Remove face 0
        dcel.faceSetRemove(setOf(0))

        assertEquals(1, dcel.faces.filter { it.edge != -1 }.size)
        // Vertices 0 and 3 are only in Face 0, so they should be removed.
        // Vertices 1, 2, 4, 5 are in Face 1, so they should remain.
        assertEquals(4, dcel.vertices.filter { it.edge != -1 }.size)
        assertEquals(-1, dcel.vertices[0].edge)
        assertEquals(-1, dcel.vertices[3].edge)
        assertNotEquals(-1, dcel.vertices[1].edge)
        assertNotEquals(-1, dcel.vertices[2].edge)

        val edges = dcel.edgesForFace(1)
        for (e in edges) {
            assertEquals(-1,dcel.halfEdges[e].otherEdge)
        }

    }

    @Test
    fun testRemoveAllFaces() {
        val positions = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(1.0, 0.0, 0.0),
            Vector3(1.0, 1.0, 0.0)
        )
        val polygons = listOf(
            IndexedPolygon(listOf(0, 1, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )
        val meshData = MeshData(VertexData(positions = positions), polygons)
        val dcel = meshData.toDcel()

        dcel.faceSetRemove(setOf(0))

        assertEquals(0, dcel.faces.filter { it.edge != -1 }.size)
        assertEquals(0, dcel.vertices.filter { it.edge != -1 }.size)
    }
    @Test
    fun testRemoveFaceWithHole() {
        val positions = listOf(
            // Outer
            Vector3(0.0, 0.0, 0.0), // 0
            Vector3(10.0, 0.0, 0.0), // 1
            Vector3(10.0, 10.0, 0.0), // 2
            Vector3(0.0, 10.0, 0.0), // 3
            // Hole
            Vector3(4.0, 4.0, 0.0), // 4
            Vector3(6.0, 4.0, 0.0), // 5
            Vector3(6.0, 6.0, 0.0), // 6
            Vector3(4.0, 6.0, 0.0)  // 7
        )
        
        val dcel = org.openrndr.extra.mesh.dcel.Dcel()
        dcel.vertices.addAll(positions.mapIndexed { index, it -> org.openrndr.extra.mesh.dcel.Vertex(it, -1) })
        
        // Outer loop
        val outerEdges = (0..3).map { i ->
            val idx = dcel.halfEdges.size
            dcel.halfEdges.add(org.openrndr.extra.mesh.dcel.HalfEdge(0, i, -1, -1, -1, IntArray(0)))
            if (dcel.vertices[i].edge == -1) dcel.vertices[i].edge = idx
            idx
        }
        for (i in 0..3) {
            dcel.halfEdges[outerEdges[i]].nextEdge = outerEdges[(i + 1) % 4]
            dcel.halfEdges[outerEdges[i]].prevEdge = outerEdges[(i + 3) % 4]
        }
        
        // Hole loop (face = -1)
        val holeEdges = (4..7).map { i ->
            val idx = dcel.halfEdges.size
            dcel.halfEdges.add(org.openrndr.extra.mesh.dcel.HalfEdge(-1, i, -1, -1, -1, IntArray(0)))
            if (dcel.vertices[i].edge == -1) dcel.vertices[i].edge = idx
            idx
        }
        // hole loop should be opposite direction or just connected
        for (i in 0..3) {
            dcel.halfEdges[holeEdges[i]].nextEdge = holeEdges[(i + 1) % 4]
            dcel.halfEdges[holeEdges[i]].prevEdge = holeEdges[(i + 3) % 4]
        }
        
        dcel.faces.add(org.openrndr.extra.mesh.dcel.Face(outerEdges[0], intArrayOf(holeEdges[0])))
        
        assertEquals(1, dcel.faces.filter { it.edge != -1 }.size)
        assertEquals(8, dcel.vertices.filter { it.edge != -1 }.size)
        
        dcel.faceSetRemove(setOf(0))
        
        assertEquals(0, dcel.faces.filter { it.edge != -1 }.size)
        // All vertices should be removed because the only face they were part of is gone.
        // Even hole vertices are only "incident" to face -1, and their otherEdge is -1.
        assertEquals(0, dcel.vertices.filter { it.edge != -1 }.size)
    }
}
