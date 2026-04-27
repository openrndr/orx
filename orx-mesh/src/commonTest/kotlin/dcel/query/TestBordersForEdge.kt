package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBordersForEdge {
    @Test
    fun testBordersForEdgeSimple() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0),
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(2.0, 1.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(positions = listOf(0, 1, 2, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 0
                IndexedPolygon(positions = listOf(1, 4, 5, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())  // Face 1
            )
        )

        val dcel = meshData.toDcel()
        
        // Edge between face 0 and face 1 should be between vertices 1 and 2 in face 0
        // and between vertices 2 and 1 in face 1 (reversed)
        
        // Let's find the edge in face 0 that goes from 1 to 2
        val face0Edges = dcel.edgeLoopIndices(dcel.faces[0].edge)
        val sharedEdgeIdx = face0Edges.find { 
            val e = dcel.halfEdges[it]
            val nextE = dcel.halfEdges[e.nextEdge]
            e.vertex == 1 && nextE.vertex == 2
        } ?: throw RuntimeException("Shared edge not found")

        val borders = dcel.bordersForEdge(sharedEdgeIdx)
        assertEquals(1, borders.size)
        assertEquals(1, borders[0].size)
        assertEquals(sharedEdgeIdx, borders[0][0])
    }

    @Test
    fun testBordersForEdgeMultiple() {
        // Two faces sharing two separate borders
        // Face 0: (0, 1, 2, 3, 4, 5)
        // Face 1: (1, 0, 6, 4, 3, 7)
        // Shared edges in Face 0: 0->1 and 3->4
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(1.0, 0.0, 0.0), // 1
                    Vector3(1.0, 1.0, 0.0), // 2
                    Vector3(0.0, 1.0, 0.0), // 3
                    Vector3(-1.0, 1.0, 0.0), // 4
                    Vector3(-1.0, 0.0, 0.0), // 5
                    Vector3(0.5, -0.5, 0.0), // 6
                    Vector3(-0.5, 1.5, 0.0)  // 7
                )
            ),
            polygons = listOf(
                IndexedPolygon(positions = listOf(0, 1, 2, 3, 4, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(positions = listOf(1, 0, 6), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(positions = listOf(4, 3, 7), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )
        )
        // Wait, the description says "two faces that share the given edge".
        // My example has 3 faces. If I call it for edge between 0 and 1, it should only return shared edges between Face 0 and Face 1.
        
        val dcel = meshData.toDcel()
        val face0Edges = dcel.edgeLoopIndices(dcel.faces[0].edge)
        val edge01 = face0Edges.find { dcel.halfEdges[it].vertex == 0 && dcel.halfEdges[dcel.halfEdges[it].nextEdge].vertex == 1 }!!
        
        val borders = dcel.bordersForEdge(edge01)
        assertEquals(1, borders.size)
        assertEquals(1, borders[0].size)
        assertEquals(edge01, borders[0][0])
    }
    
    @Test
    fun testBordersForEdgeContiguous() {
         val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(1.0, 0.0, 0.0), // 1
                    Vector3(2.0, 0.0, 0.0), // 2
                    Vector3(2.0, 1.0, 0.0), // 3
                    Vector3(1.0, 1.0, 0.0), // 4
                    Vector3(0.0, 1.0, 0.0), // 5
                    Vector3(1.0, 2.0, 0.0)  // 6
                )
            ),
            polygons = listOf(
                IndexedPolygon(positions = listOf(0, 1, 2, 3, 4, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 0
                IndexedPolygon(positions = listOf(1, 0, 5, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())       // Face 1
            )
        )
        // Face 0 edges: 0-1, 1-2, 2-3, 3-4, 4-5, 5-0
        // Face 1 edges: 1-0, 0-5, 5-4, 4-1
        // Shared edges in Face 0: 0-1 (twin is 1-0), 4-5 (twin is 5-4), 5-0 (twin is 0-5)
        // 4-5 and 5-0 are contiguous in Face 0. 0-1 is separate (unless we consider it contiguous through vertex 0)
        // Wait, 4-5, 5-0, 0-1 are contiguous in Face 0!
        // 4->5, 5->0, 0->1.
        
        val dcel = meshData.toDcel()
        val face0Edges = dcel.edgeLoopIndices(dcel.faces[0].edge)
        val edge01 = face0Edges.find { dcel.halfEdges[it].vertex == 0 && dcel.halfEdges[dcel.halfEdges[it].nextEdge].vertex == 1 }!!

        val borders = dcel.bordersForEdge(edge01)
        // Shared edges: 4-5, 5-0, 0-1. 
        // 4->5 next is 5->0. 5->0 next is 0->1.
        // So they should form ONE border of length 3.
        
        assertEquals(1, borders.size)
        assertEquals(3, borders[0].size)
    }
}
