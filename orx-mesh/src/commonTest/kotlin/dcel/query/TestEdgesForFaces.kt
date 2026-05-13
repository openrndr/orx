package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEdgesForFaces {
    @Test
    fun testEdgesForFaces() {
        val dcel = Dcel()
        // Create two adjacent triangles
        // Face 0: (0,0,0), (1,0,0), (0,1,0) - edges 0, 1, 2
        // Face 1: (1,0,0), (1,1,0), (0,1,0) - edges 3, 4, 5
        // Edge 1 (1->2) is other of Edge 5 (2->1)
        
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 2))
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), 3))

        dcel.faces.add(Face(0))
        dcel.faces.add(Face(3))

        // Face 0: 0->1, 1->2, 2->0
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, -1, IntArray(0))) // 0
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, 5, IntArray(0)))  // 1 (other: 5)
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, -1, IntArray(0))) // 2

        // Face 1: 1->3, 3->2, 2->1
        dcel.halfEdges.add(HalfEdge(1, 1, 4, 5, -1, IntArray(0))) // 3
        dcel.halfEdges.add(HalfEdge(1, 3, 5, 3, -1, IntArray(0))) // 4
        dcel.halfEdges.add(HalfEdge(1, 2, 3, 4, 1, IntArray(0)))  // 5 (other: 1)

        val edges = dcel.edgesForFaces(setOf(0, 1))
        
        // Face 0 has 3 edges, Face 1 has 3 edges. One is shared.
        // Total unique edges should be 5.
        assertEquals(5, edges.size)
        
        // Ensure that for the shared edge {1, 5}, only one is present
        val has1 = edges.contains(1)
        val has5 = edges.contains(5)
        assertEquals(true, has1 != has5, "Exactly one of the shared half-edges should be present")
    }
}
