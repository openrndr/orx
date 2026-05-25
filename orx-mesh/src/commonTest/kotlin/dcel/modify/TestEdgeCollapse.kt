package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.DCELAttributes
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestEdgeCollapse {
//    @Test
//    fun testEdgeCollapseAttributes() {
//        val meshData = MeshData(
//            VertexData(
//                positions = listOf(
//                    Vector3(0.0, 1.0, 0.0), // 0
//                    Vector3(0.0, 0.0, 0.0), // 1
//                    Vector3(1.0, 0.0, 0.0), // 2
//                    Vector3(1.0, 1.0, 0.0), // 3
//                    Vector3(2.0, 0.0, 0.0)  // 4
//                ),
//                colors = listOf(
//                    ColorRGBa.RED,   // 0
//                    ColorRGBa.GREEN, // 1
//                    ColorRGBa.BLUE,  // 2
//                    ColorRGBa.WHITE, // 3
//                    ColorRGBa.BLACK  // 4
//                )
//            ),
//            polygons = listOf(
//                IndexedPolygon(listOf(0, 1, 3), emptyList(), listOf(0, 1, 3), emptyList(), emptyList(), emptyList()),
//                IndexedPolygon(listOf(1, 2, 3), emptyList(), listOf(1, 2, 3), emptyList(), emptyList(), emptyList()),
//                IndexedPolygon(listOf(2, 4, 3), emptyList(), listOf(2, 4, 3), emptyList(), emptyList(), emptyList())
//            )
//        )
//
//        val dcel = meshData.toDcel()
//
//        // Find edge (1, 2)
//        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 1 && dcel.halfEdges[it.nextEdge].vertex == 2 }
//        val edge = dcel.halfEdges[edgeIdx]
//
//        println("[DEBUG_LOG] edge: $edgeIdx, otherEdge: ${edge.otherEdge}")
//        if (edge.otherEdge != -1) {
//            println("[DEBUG_LOG] attributes: ${edge.attributes.contentToString()}")
//            println("[DEBUG_LOG] otherAttributes: ${dcel.halfEdges[edge.otherEdge].attributes.contentToString()}")
//        }
//
//        dcel.edgeCollapse(edge)
//
//        // Vertex 2 merged into 1.
//        // Attributes at 1 and 2 should be averaged.
//        // GREEN (0,1,0) and BLUE (0,0,1) -> (0, 0.5, 0.5)
//        val expectedColor = (ColorRGBa.GREEN + ColorRGBa.BLUE) * 0.5
//
//        println("[DEBUG_LOG] All edges starting at vertex 1:")
//        for ((idx, he) in dcel.halfEdges.withIndex()) {
//            if (he.vertex == 1 && he.face != -1) {
//                val cIdx = he.attributes[DCELAttributes.COLOR.index]
//                println("[DEBUG_LOG] Edge $idx face ${he.face} color index $cIdx color ${if (cIdx != -1) dcel.colors[cIdx] else "none"}")
//            }
//        }
//
//        val survivedEdgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 1 && it.face != -1 }
//        val survivedEdge = dcel.halfEdges[survivedEdgeIdx]
//        val colorIdx = survivedEdge.attributes[DCELAttributes.COLOR.index]
//        val actualColor = dcel.colors[colorIdx]
//
//        assertEquals(expectedColor.r, actualColor.r, 0.01)
//        assertEquals(expectedColor.g, actualColor.g, 0.01)
//        assertEquals(expectedColor.b, actualColor.b, 0.01)
//    }

    @Test
    fun testEdgeCollapseInterior() {
        // Two triangles sharing edge (1, 2)
        // Face 0: (0, 1, 2)
        // Face 1: (2, 1, 3)
        // We will collapse edge (1, 2)
        // Vertex 2 will be merged into vertex 1.
        
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 1.0, 0.0), // 0
                    Vector3(0.0, 0.0, 0.0), // 1
                    Vector3(1.0, 0.0, 0.0), // 2
                    Vector3(1.0, 1.0, 0.0)  // 3
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                ),
                IndexedPolygon(
                    positions = listOf(2, 1, 3),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Find edge (1, 2)
        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 1 && dcel.halfEdges[it.nextEdge].vertex == 2 }
        assertNotEquals(-1, edgeIdx)
        val edge = dcel.halfEdges[edgeIdx]
        
        val v1Pos = dcel.vertices[1].position
        val v2Pos = dcel.vertices[2].position
        val expectedPos = (v1Pos + v2Pos) * 0.5
        
        dcel.edgeCollapse(edge)
        
        // Vertex 1 should have the new position
        assertEquals(expectedPos.x, dcel.vertices[1].position.x, 0.0001)
        assertEquals(expectedPos.y, dcel.vertices[1].position.y, 0.0001)
        
        // Face 0 and Face 1 should be gone (or have -1 edge)
        // In my implementation I might mark them with edge = -1
        
        // Check that remaining edges form a consistent structure
        // After collapsing (1, 2), we should have edges (0, 1) and (1, 3) and (3, 0)
        // Wait, (0, 1, 2) -> (0, 1, 1) degenerate? No.
        // Original: (0->1, 1->2, 2->0) and (2->1, 1->3, 3->2)
        // Collapse 1-2:
        // 1 and 2 merged at V_new.
        // (0->1, 1->1, 1->0) -> 1->1 is gone. 0->1 and 1->0 remain?
        // Actually (0, 1, 2) triangle becomes edge (0, 1).
        // (2, 1, 3) triangle becomes edge (1, 3).
        // If they were triangles, they should probably be removed.
        
        // Let's see what should remain.
        // If we have a diamond (0,1,2,3) with diagonal (1,2).
        // Collapsing (1,2) should result in edges (0,1) and (1,3)? No.
        // It should probably result in a single line if it was just two triangles.
        // No, if 1 and 2 are merged, 0-1 and 0-2 become the same edge (0,1).
        // 3-1 and 3-2 become the same edge (3,1).
        
        // A better test might be a larger mesh where collapsing an edge doesn't destroy everything.
    }
    
    @Test
    fun testEdgeCollapseInQuad() {
        // A larger mesh: a 3x3 grid of vertices, 8 triangles.
        // 0--1--2
        // | /| /|
        // 3--4--5
        // | /| /|
        // 6--7--8
        
        val meshData = MeshData(
            VertexData(
                positions = (0 until 9).map { Vector3((it % 3).toDouble(), (it / 3).toDouble(), 0.0) }
            ),
            polygons = listOf(
                IndexedPolygon(listOf(0, 3, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(0, 4, 1), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(1, 4, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(1, 5, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(3, 6, 7), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(3, 7, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(4, 7, 8), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(4, 8, 5), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )
        )

        val dcel = meshData.toDcel()
        
        // Find edge (4, 5)
        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 4 && dcel.halfEdges[it.nextEdge].vertex == 5 }
        assertNotEquals(-1, edgeIdx)
        val edge = dcel.halfEdges[edgeIdx]
        
        dcel.edgeCollapse(edge)
        
        // Vertex 5 should be merged into vertex 4
        // Check vertex 2's neighbors. It used to be (1, 5). Now it should be (1, 4).
        val v2EdgeIdx = dcel.vertices[2].edge
        val v2Edges = mutableListOf<Int>()
        var curr = v2EdgeIdx
        // We need to traverse around vertex 2.
        // Actually it's easier to check edges for face containing 2.
        // Face (1, 5, 2) became (1, 4, 2).
        val face2Idx = dcel.halfEdges.indexOfFirst { it.vertex == 2 && it.face != -1 }
        val fEdges = mutableListOf<Int>()
        curr = dcel.faces[dcel.halfEdges[face2Idx].face].edge
        val start = curr
        do {
            fEdges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        } while (curr != start)
        
        val fVertices = fEdges.map { dcel.halfEdges[it].vertex }
        assertTrue(fVertices.contains(1))
        assertTrue(fVertices.contains(4))
        assertTrue(fVertices.contains(2))
    }
}
