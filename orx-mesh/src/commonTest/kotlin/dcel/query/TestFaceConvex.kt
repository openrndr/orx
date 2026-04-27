package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TestFaceConvex {
    @Test
    fun testTriangle() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 2))
        
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, -1, IntArray(5) { -1 }))
        
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isFaceConvex(0))
    }

    @Test
    fun testSquare() {
        val dcel = Dcel()
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 3))
        
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 3, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 2, 3, 1, -1, IntArray(5) { -1 }))
        dcel.halfEdges.add(HalfEdge(0, 3, 0, 2, -1, IntArray(5) { -1 }))
        
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isFaceConvex(0))
    }

    @Test
    fun testConcaveL() {
        val dcel = Dcel()
        // 0(0,0), 1(2,0), 2(2,1), 3(1,1), 4(1,2), 5(0,2)
        val pts = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(2.0, 0.0, 0.0),
            Vector3(2.0, 1.0, 0.0),
            Vector3(1.0, 1.0, 0.0), // Reflex vertex
            Vector3(1.0, 2.0, 0.0),
            Vector3(0.0, 2.0, 0.0)
        )
        
        pts.forEachIndexed { i, p -> dcel.vertices.add(Vertex(p, i)) }
        
        val n = pts.size
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        
        dcel.faces.add(Face(0))
        
        assertFalse(dcel.isFaceConvex(0))
    }

    @Test
    fun testCollinearConvex() {
        val dcel = Dcel()
        // Triangle with a collinear point on one edge
        val pts = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(1.0, 0.0, 0.0),
            Vector3(2.0, 0.0, 0.0), // Collinear
            Vector3(1.0, 1.0, 0.0)
        )
        
        pts.forEachIndexed { i, p -> dcel.vertices.add(Vertex(p, i)) }
        
        val n = pts.size
        for (i in 0 until n) {
            dcel.halfEdges.add(HalfEdge(0, i, (i + 1) % n, (i + n - 1) % n, -1, IntArray(5) { -1 }))
        }
        
        dcel.faces.add(Face(0))
        
        assertTrue(dcel.isFaceConvex(0))
    }
}
