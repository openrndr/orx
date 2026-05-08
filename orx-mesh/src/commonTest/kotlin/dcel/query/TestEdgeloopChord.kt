package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

class TestEdgeloopChord {
    @Test
    fun testSquareChord() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    listOf(0, 1, 2, 3),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        val loop = dcel.edgeLoopIndices(0)
        
        // Chord between midpoint of edge 0 (0-1) and midpoint of edge 2 (2-3)
        // Edge 0: (0.5, 0, 0), Edge 2: (0.5, 1, 0). Vertical line, inside.
        assertTrue(dcel.isEdgeloopChord(loop[0], 0.5, loop[2], 0.5), "Square vertical chord should be valid")
        
        // Chord between midpoint of edge 0 and vertex 2 (start of edge 2)
        // Edge 0: (0.5, 0, 0), Edge 2 start: (1, 1, 0)
        assertTrue(dcel.isEdgeloopChord(loop[0], 0.5, loop[2], 0.0), "Square diagonal chord should be valid")
        
        // Chord on the same edge should be false (or maybe true if we allow it, but usually a chord connects different points on the boundary)
        assertFalse(dcel.isEdgeloopChord(loop[0], 0.5, loop[0], 0.5), "Same point chord should be invalid")
        
        // Different T on same edge:
        assertTrue(dcel.isEdgeloopChord(loop[0], 0.2, loop[0], 0.8), "Same edge chord should be valid")
    }

    @Test
    fun testSquareChordCollinear() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(0.25, 0.0, 0.0),
                    Vector3(0.5, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 0.25, 0.0),
                    Vector3(1.0, 0.5, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.75, 1.0, 0.0),
                    Vector3(0.5, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0),
                    Vector3(0.0, 0.75, 0.0),
                    Vector3(0.0, 0.5, 0.0)

                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        val loop = dcel.edgeLoopIndices(0)

        // Chord between midpoint of edge 0 (0-1) and midpoint of edge 2 (2-3)
        // Edge 0: (0.5, 0, 0), Edge 2: (0.5, 1, 0). Vertical line, inside.
        assertFalse(dcel.isEdgeloopChord(loop[0], 0.5, loop[2], 0.5), "Square vertical chord should be valid")


    }

    @Test
    fun testConcaveChord() {
         val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(2.0, 2.0, 0.0), // 2
                    Vector3(1.1, 1.1, 0.0), // 3 (reflex vertex, pushed in)
                    Vector3(0.0, 2.0, 0.0)  // 4
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    listOf(0, 1, 2, 3, 4),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        val loop = dcel.edgeLoopIndices(0)
        
        // Chord from (2, 0.5) [edge 1] to (0, 0.5) [edge 4]
        // Edge 1 is (2,0) to (2,2). T=0.25 is (2, 0.5).
        // Edge 4 is (0,2) to (0,0). T=0.75 is (0, 0.5).
        // y=0.5 is well below the notch at (1.1, 1.1).
        assertTrue(dcel.isEdgeloopChord(loop[1], 0.25, loop[4], 0.75), "Concave lower chord should be valid")
        
        // Chord that crosses the notch:
        // Chord from (2, 1.5) [edge 1] to (0, 1.5) [edge 4].
        // Line y=1.5. Notch is at (1.1, 1.1).
        // The line y=1.5 crosses the edges (2,2)-(1.1,1.1) and (1.1,1.1)-(0,2).
        assertFalse(dcel.isEdgeloopChord(loop[1], 0.75, loop[4], 0.25), "Chord crossing notch boundary should be invalid")

        // Midpoint check: Chord from (2, 0.8) to (0, 1.2).
        // Midpoint is (1.0, 1.0). (1.1, 1.1) is pushed in. 
        // Depending on shape, (1.0, 1.0) might be outside.
    }
}
