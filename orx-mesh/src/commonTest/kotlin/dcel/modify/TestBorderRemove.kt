package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.query.bordersForEdge
import org.openrndr.extra.mesh.dcel.query.edgeForFaces
import org.openrndr.extra.mesh.dcel.query.edgesForFace
import org.openrndr.extra.mesh.dcel.query.faceCount
import org.openrndr.extra.mesh.dcel.query.wholeEdgeCount
import org.openrndr.extra.mesh.dcel.validate.isEulerMesh
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestBorderRemove {

    @Test
    fun testBorderRemoveSimple() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 1)
        val dcel = grid.toDcel()
        val edgeToRemove = dcel.edgeForFaces(0, 1)


        assertEquals(2, dcel.faceCount())
        assertEquals(7, dcel.wholeEdgeCount())
        assertNotEquals(-1, edgeToRemove)

        val borders = dcel.bordersForEdge(edgeToRemove)
        assertEquals(1, borders.size)
        assertEquals(1, borders.first().size)

        dcel.bordersRemove(borders)
        assertEquals(1, dcel.faceCount())
        assertEquals(6, dcel.wholeEdgeCount())

        for (face in dcel.faces.indices) {
            println("Face ${face}: ${dcel.edgesForFace(face).size} edges")
        }

        assertTrue(dcel.isEulerMesh())

    }


    @Test
    fun testBordersRemove() {
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 3, 1)
        val dcel = grid.toDcel()

        // Faces: [0] [1] [2]
        // Shared edges between 0 and 1: 1 border (1 edge)
        // Shared edges between 1 and 2: 1 border (1 edge)
        
        // Find edge between 0 and 1
        val e01Idx = dcel.halfEdges.indexOfFirst { it.face == 0 && it.otherEdge != -1 && dcel.halfEdges[it.otherEdge].face == 1 }
        assertTrue(e01Idx != -1)
        
        val borders = dcel.bordersForEdge(e01Idx)
        assertEquals(1, borders.size)
        assertEquals(1, borders[0].size)
        val previousEdgeCount = dcel.wholeEdgeCount()

        dcel.bordersRemove(borders)

        assertEquals(previousEdgeCount - 1, dcel.wholeEdgeCount())
        assertTrue(dcel.isEulerMesh())
        
        // Faces 0 and 1 should be merged
        val activeFaces = dcel.halfEdges.map { it.face }.distinct().filter { it != -1 }.sorted()
        assertEquals(2, activeFaces.size) // Was 3, now 2 (0 and 2 probably, or whatever edgeRemove does)
        
        // Now find border between the merged face and face 2
        val mergedFaceIdx = dcel.halfEdges[dcel.halfEdges.indexOfFirst { it.vertex == 0 }].face 
        val eMerged2Idx = dcel.halfEdges.indexOfFirst { it.face == mergedFaceIdx && it.otherEdge != -1 && dcel.halfEdges[it.otherEdge].face == 2 }
        assertTrue(eMerged2Idx != -1)
        
        val borders2 = dcel.bordersForEdge(eMerged2Idx)
        dcel.bordersRemove(borders2)
        assertTrue(dcel.isEulerMesh())
        
        val activeFacesFinal = dcel.halfEdges.map { it.face }.distinct().filter { it != -1 }
        assertEquals(1, activeFacesFinal.size)
    }

    @Test
    fun testBordersRemoveMultiple() {
        // Grid 2x2
        // F0 F1
        // F2 F3
        val grid = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 2)
        val dcel = grid.toDcel()
        
        // Merge F0 and F1
        val e01Idx = dcel.halfEdges.indexOfFirst { it.face == 0 && it.otherEdge != -1 && dcel.halfEdges[it.otherEdge].face == 1 }
        val borders01 = dcel.bordersForEdge(e01Idx)
        dcel.bordersRemove(borders01)
        assertTrue(dcel.isEulerMesh())
        
        // Merge (F0+F1) and F2
        val fMergedIdx = dcel.halfEdges[e01Idx].face // Oops, e01.face might be -1 now.
        val someEdgeInMergedFaceIdx = dcel.halfEdges.indexOfFirst { it.face != -1 && it.face != 2 && it.face != 3 }
        val fMerged = dcel.halfEdges[someEdgeInMergedFaceIdx].face
        
        val eMerged2Idx = dcel.halfEdges.indexOfFirst { it.face == fMerged && it.otherEdge != -1 && dcel.halfEdges[it.otherEdge].face == 2 }
        val bordersMerged2 = dcel.bordersForEdge(eMerged2Idx)
        dcel.bordersRemove(bordersMerged2)
        assertTrue(dcel.isEulerMesh())
        
        // Now we should have 2 faces left: merged(0,1,2) and 3
        val activeFaces = dcel.halfEdges.map { it.face }.distinct().filter { it != -1 }
        assertEquals(2, activeFaces.size)
    }
}
