package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEdgeSetChamfer {
    @Test
    fun testChamferSingleEdge() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 1)
        val dcel = grid.toDcel()

        // Find the shared edge between face 0 and face 1
        val sharedEdgeIdx = dcel.halfEdges.indexOfFirst { e ->
            e.face == 0 && e.otherEdge != -1 && dcel.halfEdges[e.otherEdge].face == 1
        }
        assertTrue(sharedEdgeIdx != -1, "Shared edge not found")

        val initialFaceCount = dcel.faces.size
        val newFaceIds = dcel.edgeSetChamfer(setOf(sharedEdgeIdx), 10.0)

        // For an interior edge chamfer, we expect 1 new face (the chamfer face)
        assertEquals(1, newFaceIds.size)
        assertEquals(initialFaceCount + 1, dcel.faces.size)
        
        // The new face should be a quad (4 edges)
        val newFaceId = newFaceIds.first()
        val edges = dcel.halfEdges.filter { it.face == newFaceId }
        assertEquals(4, edges.size)
    }

    @Test
    fun testChamferBoundaryEdge() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 1, 1)
        val dcel = grid.toDcel()

        // Find a boundary edge (otherEdge == -1)
        val boundaryEdgeIdx = dcel.halfEdges.indexOfFirst { it.face == 0 && it.otherEdge == -1 }
        assertTrue(boundaryEdgeIdx != -1, "Boundary edge not found")

        val initialFaceCount = dcel.faces.size
        val newFaceIds = dcel.edgeSetChamfer(setOf(boundaryEdgeIdx), 10.0)

        // For a boundary edge, we don't expect a new face in the DCEL (it's "outside")
        // but the edge itself is split.
        assertEquals(0, newFaceIds.size)
        assertEquals(initialFaceCount, dcel.faces.size)
    }

    @Test
    fun testChamferMultipleEdges() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 3, 1)
        val dcel = grid.toDcel()

        // Find the two shared edges
        val sharedEdges = dcel.halfEdges.filter { e ->
            e.face != -1 && e.otherEdge != -1 && dcel.halfEdges[e.otherEdge].face != -1 && e.face < dcel.halfEdges[e.otherEdge].face
        }.map { dcel.halfEdges.indexOf(it) }.toSet()

        assertEquals(2, sharedEdges.size)

        val initialFaceCount = dcel.faces.size
        val newFaceIds = dcel.edgeSetChamfer(sharedEdges, 10.0)

        assertEquals(2, newFaceIds.size)
        assertEquals(initialFaceCount + 2, dcel.faces.size)

        for (faceId in newFaceIds) {
            val edges = dcel.halfEdges.filter { it.face == faceId }
            assertEquals(4, edges.size)
        }
    }
}
