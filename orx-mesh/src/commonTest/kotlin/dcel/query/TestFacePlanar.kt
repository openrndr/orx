package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TestFacePlanar {
    @Test
    fun testPlanarSquare() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))
        
        val n = 4
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isEdgeLoopPlanar(0))
    }

    @Test
    fun testNonPlanarQuad() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.1), 2)) // Slightly out of plane
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))
        
        val n = 4
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        dcel.faces.add(Face(0))
        
        assertFalse(dcel.isEdgeLoopPlanar(0))
    }

    @Test
    fun testPlanarTriangle() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 1.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 2.0), 2))
        
        val n = 3
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isEdgeLoopPlanar(0))
    }

    @Test
    fun testCollinearPoints() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(2.0, 0.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(3.0, 0.0, 0.0), 3))
        
        val n = 4
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isEdgeLoopPlanar(0))
    }
}
