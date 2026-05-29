package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.navigate.filterEdges
import org.openrndr.extra.mesh.dcel.navigate.isBoundary
import org.openrndr.extra.mesh.dcel.query.edgeCount
import org.openrndr.extra.mesh.dcel.query.edgesForFaces
import org.openrndr.extra.mesh.dcel.query.faceWinding
import org.openrndr.extra.mesh.dcel.query.vertexCount
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Winding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEdgeSetOffset {
    @Test
    fun testSingleEdgeOffset() {
        val dcel = Dcel()
        // Create a single triangle (0,0,0), (1,0,0), (0,1,0)
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 2))

        dcel.faces.add(Face(0))

        // inner edges
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, 3, IntArray(0)))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, 4, IntArray(0)))
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, 5, IntArray(0)))

        // boundary edges (initially none, let's add them to have something to offset)
        // e3: 1 -> 0
        dcel.halfEdges.add(HalfEdge(-1, 1, -1, -1, 0, IntArray(0)))
        // e4: 2 -> 1
        dcel.halfEdges.add(HalfEdge(-1, 2, -1, -1, 1, IntArray(0)))
        // e5: 0 -> 2
        dcel.halfEdges.add(HalfEdge(-1, 0, -1, -1, 2, IntArray(0)))
        
        // Link boundary edges next/prev
        dcel.halfEdges[3].nextEdge = 5
        dcel.halfEdges[3].prevEdge = 4
        dcel.halfEdges[4].nextEdge = 3
        dcel.halfEdges[4].prevEdge = 5
        dcel.halfEdges[5].nextEdge = 4
        dcel.halfEdges[5].prevEdge = 3

        // Offset edge 3 (from (1,0,0) to (0,0,0))
        // Normal of face 0 is (1,0,0) x (0,1,0) = (0,0,1)
        // Edge direction (0,0,0) - (1,0,0) = (-1,0,0)
        // Offset normal = (-1,0,0) x (0,0,1) = (0,1,0)
        // Offset by 1.0 should move it to y=1.0
        
        val newFaces = dcel.edgeSetOffset(setOf(3), 1.0)
        
        assertEquals(1, newFaces.size)
        val nfIdx = newFaces.first()
        val nf = dcel.faces[nfIdx]
        
        // Verify vertices of the new face
        // Original edge was 1 -> 0
        // New vertices should be (1, 1, 0) and (0, 1, 0)
        
        // Find the outer edge of the new face (should have no otherEdge initially or it might be set)
        // In my implementation e0 was the outer edge
        val e0Idx = dcel.faces[nfIdx].edge
        val e0 = dcel.halfEdges[e0Idx]
        val v0 = dcel.vertices[e0.vertex].position
        val v1 = dcel.vertices[dcel.halfEdges[e0.nextEdge].vertex].position
        
        // v0 should be offset from (1,0,0) by (0,-1,0) -> (1,-1,0)
        // v1 should be offset from (0,0,0) by (0,-1,0) -> (0,-1,0)
        
        // Note: order might depend on implementation. 
        // My implementation: e0: nv0 -> nv1
        assertEquals(Vector3(1.0, -1.0, 0.0), v0)
        assertEquals(Vector3(0.0, -1.0, 0.0), v1)
    }

    @Test
    fun testOffsetJoins() {
        val shape = regularPolygon(6, Vector2.ZERO, 200.0).shape
        val dcel = shapeToDcelNoTriangulation(shape, 0.5)
        assertEquals(Winding.CLOCKWISE, dcel.faceWinding(0))
        val faces = dcel.edgeSetOffset(setOf(0,1,2,3,4,5), -20.0, useJoins = true)

        assertTrue(dcel.isEulerMesh())
        assertTrue(faces.all { dcel.faceWinding(it) == Winding.CLOCKWISE })
        assertEquals(12, faces.size)
    }

//    @Test
//    fun testOffsetJoinsTwoIterations() {
//        val shape = regularPolygon(6, Vector2.ZERO, 200.0).shape
//        val dcel = shapeToDcelNoTriangulation(shape, 0.5)
//        assertEquals(Winding.CLOCKWISE, dcel.faceWinding(0))
//        val faces = dcel.edgeSetOffset(setOf(0,1,2,3,4,5), -20.0, useJoins = true)
//
//        assertTrue(dcel.isEulerMesh())
//        assertTrue(faces.all { dcel.faceWinding(it) == Winding.CLOCKWISE })
//        assertEquals(12, faces.size)
//
//        val edges = with(dcel) {
//            edgesForFaces(faces).toList().filterEdges { it.isBoundary }
//        }
//        assertEquals(12, edges.size)
//
//        val faces2 = dcel.edgeSetOffset(edges.toSet(), 20.0, useJoins = true)
//        assertEquals(24, faces2.size)
//    }

    @Test
    fun testContiguousEdgesOffset() {
        val dcel = Dcel()
        // Create a square from two triangles
        // (0,1) -- (1,1)
        //   |    /   |
        // (0,0) -- (1,0)
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), -1)) // 0
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), -1)) // 1
        dcel.vertices.add(Vertex(Vector3(1.0, 1.0, 0.0), -1)) // 2
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), -1)) // 3

        dcel.faces.add(Face(0))
        dcel.faces.add(Face(1))

        // Face 0: 0->1, 1->2, 2->0
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, -1, IntArray(0))) // 0: 0->1
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, 3, IntArray(0)))  // 1: 1->2
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, -1, IntArray(0))) // 2: 2->0

        // Face 1: 0->2, 2->3, 3->0
        dcel.halfEdges.add(HalfEdge(1, 0, 4, 5, 1, IntArray(0)))  // 3: 0->2
        dcel.halfEdges.add(HalfEdge(1, 2, 5, 3, -1, IntArray(0))) // 4: 2->3
        dcel.halfEdges.add(HalfEdge(1, 3, 3, 4, -1, IntArray(0))) // 5: 3->0

        dcel.faces[0].edge = 0
        dcel.faces[1].edge = 3
        dcel.vertices[0].edge = 0
        dcel.vertices[1].edge = 1
        dcel.vertices[2].edge = 2
        dcel.vertices[3].edge = 5

        // Add boundary edges
        // e6: 1->0 (other of 0)
        dcel.halfEdges.add(HalfEdge(-1, 1, -1, -1, 0, IntArray(0))) // 6
        // e7: 0->3 (other of 5)
        dcel.halfEdges.add(HalfEdge(-1, 0, -1, -1, 5, IntArray(0))) // 7
        // e8: 3->2 (other of 4)
        dcel.halfEdges.add(HalfEdge(-1, 3, -1, -1, 4, IntArray(0))) // 8
        // e9: 2->1 (other of 1?? no, 1's other is 3)
        // Wait, 1: 1->2, its other is 3: 0->2. No, other of 1 (1->2) should be 2->1.
        // Let's fix connectivity.
        // e0: 0->1. other: e6: 1->0
        dcel.halfEdges[0].otherEdge = 6
        // e1: 1->2. other: e9: 2->1
        dcel.halfEdges.add(HalfEdge(-1, 2, -1, -1, 1, IntArray(0))) // 9
        dcel.halfEdges[1].otherEdge = 9
        // e2: 2->0. other: 3: 0->2
        dcel.halfEdges[2].otherEdge = 3
        dcel.halfEdges[3].otherEdge = 2
        // e4: 2->3. other: 8: 3->2
        dcel.halfEdges[4].otherEdge = 8
        // e5: 3->0. other: 7: 0->3
        dcel.halfEdges[5].otherEdge = 7

        // Boundary: 6 (1->0), 7 (0->3), 8 (3->2), 9 (2->1)
        // Chain: 6, 7, 8, 9
        dcel.halfEdges[6].nextEdge = 7; dcel.halfEdges[6].prevEdge = 9
        dcel.halfEdges[7].nextEdge = 8; dcel.halfEdges[7].prevEdge = 6
        dcel.halfEdges[8].nextEdge = 9; dcel.halfEdges[8].prevEdge = 7
        dcel.halfEdges[9].nextEdge = 6; dcel.halfEdges[9].prevEdge = 8

        // Offset 6 and 7.
        // 6: 1->0. pos: (1,0) to (0,0). Normal: (0,-1,0)
        // 7: 0->3. pos: (0,0) to (0,1). Normal: (-1,0,0)
        // Common vertex: 0. Offset should be ((-1, -1) normalized) * miter * offset
        // miter = 1/cos(45) = sqrt(2)
        // offset = 1.0. New pos for vertex 0: (0,0) + (-1, -1) = (-1, -1)
        
        val newFaces = dcel.edgeSetOffset(setOf(6, 7), 1.0)
        assertEquals(2, newFaces.size)
        
        // Vertex 0 was at (0,0,0). It is shared by 6 and 7.
        // Chain is [6, 7]
        // originalVertexIndices [1, 0, 3]
        // newVertexIndices: nv0 (for v1), nv1 (for v0), nv2 (for v3)
        // nv1 should be at (0,0,0) + (-1,-1,0) = (-1,-1,0)

        // Count vertices: 4 original + 1 (for e9) + 3 new = 8 total? No.
        // Let's check how many vertices were there before offset call.
        // dcel.vertices.add (0, 1, 2, 3) -> 4 vertices.
        // dcel.halfEdges.add (0, 1, 2, 3, 4, 5) -> edges.
        // dcel.halfEdges.add (6, 7, 8) -> 7, 8, 9. Wait, I added 6, 7, 8, 9.
        // Vertex was not added for boundary edges, they use existing vertices.
        // So before offset, there are 4 vertices.
        // offset(6,7) adds 3 new vertices.
        // v4 (for v1), v5 (for v0), v6 (for v3).
        // Vertex 0 was the second one in chain original vertices, so it should be the second new vertex added.
        // Index should be 4 + 1 = 5.

        val nv1Pos = dcel.vertices[5].position
        assertTrue((nv1Pos - Vector3(-1.0, -1.0, 0.0)).length < 1e-6)
    }

    @Test
    fun testChainingDifferentFaces() {
        val dcel = Dcel()
        // Two disjoint triangles sharing a vertex
        // T1: (0,0) -> (1,0) -> (0,1)
        // T2: (2,0) -> (0,0) -> (2,1)
        // They share vertex (0,0).
        
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), -1)) // 0: Shared
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), -1)) // 1
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), -1)) // 2
        
        dcel.vertices.add(Vertex(Vector3(2.0, 0.0, 0.0), -1)) // 3
        dcel.vertices.add(Vertex(Vector3(2.0, 1.0, 0.0), -1)) // 4

        // Face 0: 0->1, 1->2, 2->0
        dcel.faces.add(Face(-1))
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, -1, IntArray(0))) // 0: 0->1
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, -1, IntArray(0))) // 1: 1->2
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, -1, IntArray(0))) // 2: 2->0
        dcel.faces[0].edge = 0

        // Face 1: 3->0, 0->4, 4->3
        dcel.faces.add(Face(-1))
        dcel.halfEdges.add(HalfEdge(1, 3, 4, 5, -1, IntArray(0))) // 3: 3->0
        dcel.halfEdges.add(HalfEdge(1, 0, 5, 3, -1, IntArray(0))) // 4: 0->4
        dcel.halfEdges.add(HalfEdge(1, 4, 3, 4, -1, IntArray(0))) // 5: 4->3
        dcel.faces[1].edge = 3

        // Edge 0 (0->1) and Edge 4 (0->4) are NOT chained by DCEL next/prev.
        // But edge 3 ends at 0, and edge 0 starts at 0.
        // Also edge 3 ends at 0, and edge 4 starts at 0.
        // So [3, 0] should be a chain.
        // And [3, 4] should be a chain.
        
        // Let's offset setOf(3, 0).
        // Edge 3: 3->0. Edge 0: 0->1.
        // They share vertex 0.
        
        val faces = dcel.edgeSetOffset(setOf(3, 0), 1.0)
        // If they are chained, they should produce 2 faces in ONE chain processing.
        // Actually the number of faces is same whether chained or not, but vertex 0 will be offset ONCE if chained.
        // If not chained, vertex 0 will be offset twice (separately for each edge).
        
        assertEquals(2, faces.size)
        // Since they are chained, there should be 3 new vertices total (v5 for start of 3, v6 for shared 0, v7 for end of 0).
        // Wait, 5 vertices originally (0,1,2,3,4).
        // New vertices: 5, 6, 7.
        assertEquals(8, dcel.vertices.size)
    }
    @Test
    fun testCheckFaceFilter() {
        val dcel = Dcel()
        // Create a single triangle (0,0,0), (1,0,0), (0,1,0)
        dcel.vertices.add(Vertex(Vector3(0.0, 0.0, 0.0), 0))
        dcel.vertices.add(Vertex(Vector3(1.0, 0.0, 0.0), 1))
        dcel.vertices.add(Vertex(Vector3(0.0, 1.0, 0.0), 2))

        dcel.faces.add(Face(0))
        dcel.halfEdges.add(HalfEdge(0, 0, 1, 2, 3, IntArray(0)))
        dcel.halfEdges.add(HalfEdge(0, 1, 2, 0, 4, IntArray(0)))
        dcel.halfEdges.add(HalfEdge(0, 2, 0, 1, 5, IntArray(0)))

        dcel.halfEdges.add(HalfEdge(-1, 1, 4, 5, 0, IntArray(0))) // 3: 1->0
        dcel.halfEdges.add(HalfEdge(-1, 2, 5, 3, 1, IntArray(0))) // 4: 2->1
        dcel.halfEdges.add(HalfEdge(-1, 0, 3, 4, 2, IntArray(0))) // 5: 0->2

        val edgesBefore = dcel.edgeCount()
        val verticesBefore = dcel.vertexCount()
        // Offset all 3 boundary edges
        // checkFace returns false for all, so no new faces should be created
        val newFaces = dcel.edgeSetOffset(setOf(3, 4, 5), 1.0, checkFace = { false })


        assertEquals(verticesBefore, dcel.vertexCount())
        assertEquals(edgesBefore, dcel.edgeCount())
        assertEquals(0, newFaces.size, "No faces should have been created because checkFace always returns false")
    }
}
