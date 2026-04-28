package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestEdgeInsert {
    @Test
    fun testInsertEdgeInQuad() {
        // Create a 1x1 grid (one quadrilateral face)
        val mesh = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 1, 1)
        val dcel = mesh.toDcel()

        assertTrue(dcel.isEulerMesh())
        assertEquals(1, dcel.faces.size)
        assertEquals(4, dcel.halfEdges.filter { it.face == 0 }.size)

        // The quad has 4 edges. Let's find two non-adjacent edges.
        // In a simple loop: e0 -> e1 -> e2 -> e3 -> e0
        // We want to insert between e0 and e2.
        
        val face0 = dcel.faces[0]
        val e0Idx = face0.edge
        val e1Idx = dcel.halfEdges[e0Idx].nextEdge
        val e2Idx = dcel.halfEdges[e1Idx].nextEdge
        val e3Idx = dcel.halfEdges[e2Idx].nextEdge
        
        assertEquals(e0Idx, dcel.halfEdges[e3Idx].nextEdge)

        // Insert edge between e0 and e2
        // e0.vertex is the start vertex of e0.
        // wait, edgeInsert(start: Int, end: Int) uses edge indices.
        // Usually edge insert in DCEL means connecting the START vertex of 'start' edge
        // to the START vertex of 'end' edge, or something similar.
        // Let's look at the comments in EdgeInsert.kt again.
        // // the start edge and the end edge are part of the same face
        // // the start edge and the end edge are not each other's neighbors
        // // inserting the edge will split the face

        dcel.edgeInsert(e0Idx, e2Idx)
        assertTrue(dcel.isEulerMesh())

        // After split, we should have 2 faces
        assertEquals(2, dcel.faces.size)
        
        // Each new face should be a triangle (3 edges)
        val face0Edges = dcel.halfEdges.filter { it.face == 0 }
        val face1Edges = dcel.halfEdges.filter { it.face == 1 }
        
        assertEquals(3, face0Edges.size)
        assertEquals(3, face1Edges.size)
        
        // Total half edges should be 4 + 2 = 6 (plus any boundary edges if it was open)
        // gridMesh creates 1 quad. 
        // toDcel() creates half-edges for each face, but doesn't create boundary half-edges (face = -1).
        // So for 1 quad, we have 4 half-edges.
        // After split, we add 2 more half-edges. Total = 6.
        assertEquals(6, dcel.halfEdges.size)

        // Verify connectivity of the new edges
        val he0Idx = 4
        val he1Idx = 5
        val he0 = dcel.halfEdges[he0Idx]
        val he1 = dcel.halfEdges[he1Idx]
        
        assertEquals(he1Idx, he0.otherEdge)
        assertEquals(he0Idx, he1.otherEdge)
        assertEquals(0, he0.face)
        assertEquals(1, he1.face)
        
        // Verify loops
        fun checkLoop(startIdx: Int) {
            var curr = startIdx
            val face = dcel.halfEdges[curr].face
            var count = 0
            do {
                assertEquals(face, dcel.halfEdges[curr].face)
                val nextIdx = dcel.halfEdges[curr].nextEdge
                assertEquals(curr, dcel.halfEdges[nextIdx].prevEdge)
                curr = nextIdx
                count++
                if (count > 10) throw Exception("Infinite loop detected")
            } while (curr != startIdx)
            assertEquals(3, count)
        }
        
        checkLoop(he0Idx)
        checkLoop(he1Idx)
    }

    @Test
    fun testInsertEdgeInPentagon() {
        // Create a 5-sided polygon by inserting an edge into a quad then another? 
        // Or just trust the 1x1 grid test for basic logic.
        // Let's try a 2x1 grid and insert an edge in one of the quads.
        val mesh = gridMesh(Rectangle(0.0, 0.0, 200.0, 100.0), 2, 1)
        val dcel = mesh.toDcel()
        
        assertEquals(2, dcel.faces.size)
        
        // Face 0 is the first quad
        val e0 = dcel.faces[0].edge
        val e1 = dcel.halfEdges[e0].nextEdge
        val e2 = dcel.halfEdges[e1].nextEdge
        
        dcel.edgeInsert(e0, e2)
        assertTrue(dcel.isEulerMesh())
        
        assertEquals(3, dcel.faces.size)
    }
}
