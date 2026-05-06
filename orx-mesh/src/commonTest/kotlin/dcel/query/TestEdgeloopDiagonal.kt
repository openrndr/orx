package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestEdgeloopDiagonal {
    @Test
    fun testConvexSquare() {
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
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        
        // Edges: 0->1 (0), 1->2 (1), 2->3 (2), 3->0 (3) (assuming toDcel works this way)
        // Let's find edges by vertices if we can, or just guess based on loop
        val loop = dcel.edgeLoopIndices(0)
        assertEquals(4, loop.size)
        
        // Diagonals in a square: 0-2 and 1-3
        assertTrue(dcel.isEdgeloopDiagonal(loop[0], loop[2]))
        assertTrue(dcel.isEdgeloopDiagonal(loop[1], loop[3]))
        
        // Neighbors are not diagonals
        assertFalse(dcel.isEdgeloopDiagonal(loop[0], loop[1]))
        assertFalse(dcel.isEdgeloopDiagonal(loop[1], loop[2]))
        assertFalse(dcel.isEdgeloopDiagonal(loop[2], loop[3]))
        assertFalse(dcel.isEdgeloopDiagonal(loop[3], loop[0]))
    }

    @Test
    fun testConcaveShape() {
        // L-shape or Pacman-like shape
        // 0:(0,0), 1:(2,0), 2:(2,2), 3:(1,1), 4:(0,2)
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(2.0, 2.0, 0.0), // 2
                    Vector3(1.1, 1.1, 0.0), // 3 (reflex vertex)
                    Vector3(0.0, 2.0, 0.0)  // 4
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    listOf(0, 1, 2, 3, 4),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        val loop = dcel.edgeLoopIndices(0)
        
        // Diagonal 0-2: passes through 1.1, 1.1? 
        // 0:(0,0), 2:(2,2). Line is y=x. 3 is (1.1, 1.1) which is on y=x.
        // Actually (1.1, 1.1) is INSIDE the triangle (0,0), (2,0), (2,2)
        // Since it passes through vertex 3, it should return false.
        assertFalse(dcel.isEdgeloopDiagonal(loop[0], loop[2]))
        
        // Let's try 0-3. (0,0) to (1.1, 1.1).
        // It's a valid diagonal if it doesn't cross any edges.
        assertTrue(dcel.isEdgeloopDiagonal(loop[0], loop[3]))
        
        // Diagonal 1-4: (2,0) to (0,2). Line is y = -x + 2.
        // Vertex 3 is (1.1, 1.1). 1.1 = -1.1 + 2 = 0.9. No, 1.1 > 0.9, so 3 is "above" the line 1-4.
        // Point (1,1) is on the line. (1.1, 1.1) is further out.
        // In this shape, 1-4 should be OUTSIDE because 3 is "pushed in".
        // Let's use a more extreme value for vertex 3 to be sure.
        // If 3 is (0.5, 0.5), it's definitely pushing in.
        // Current 3 is (1.1, 1.1). (1,1) is the midpoint of (2,0)-(0,2).
        // Since (1.1, 1.1) is "further" from the origin than (1,1), it is OUTSIDE the triangle (0,0),(2,0),(0,2).
        // BUT the polygon is (0,0)->(2,0)->(2,2)->(1.1,1.1)->(0,2)->(0,0).
        // Midpoint of 1-4 is (1,1). 
        // Is (1,1) inside this polygon?
        // Let's check with a ray from (1,1) to (-1, -1).
        // 1-4: (2,0)-(0,2). 
        // Actually, if we use (0.5, 0.5) for vertex 3, it will be definitely concave and 1-4 will be outside.
        
        // Let's just adjust the test case to be more obviously concave for 1-4.
        // (0,0), (2,0), (2,2), (0.1, 0.1), (0,2)
        // Midpoint of (2,0)-(0,2) is (1,1). (0.1, 0.1) is very much "inside" from (1,1).
        // No, wait. 
        
        val meshData2 = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(2.0, 2.0, 0.0), // 2
                    Vector3(0.5, 0.5, 0.0), // 3 (reflex vertex, pushed in)
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
        val dcel2 = meshData2.toDcel()
        val loop2 = dcel2.edgeLoopIndices(0)
        // Diagonal 1-4: (2,0) to (0,2). Midpoint (1,1).
        // 3 is (0.5, 0.5). 1-4 passes "outside" the notch at 3.
        assertFalse(dcel2.isEdgeloopDiagonal(loop2[1], loop2[4]))
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        kotlin.test.assertEquals(expected, actual)
    }
}
