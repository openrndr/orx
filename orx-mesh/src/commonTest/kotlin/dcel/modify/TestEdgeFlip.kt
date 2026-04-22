package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestEdgeFlip {
    @Test
    fun testEdgeFlip() {
        // Two triangles sharing edge (0, 1)
        // Face 0: (0, 1, 2)
        // Face 1: (1, 0, 3)
        // Vertex positions for reference (though Dcel only uses Vector2):
        // 0: (0, 0)
        // 1: (1, 0)
        // 2: (1, 1)
        // 3: (0, -1)
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(1.0, 0.0, 0.0), // 1
                    Vector3(1.0, 1.0, 0.0), // 2
                    Vector3(0.0, -1.0, 0.0) // 3
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
                    positions = listOf(1, 0, 3),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Find the edge (0, 1) or (1, 0)
        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 0 && dcel.halfEdges[it.nextEdge].vertex == 1 }
        assertNotEquals(-1, edgeIdx)
        val edge = dcel.halfEdges[edgeIdx]
        
        dcel.edgeFlip(edge)
        
        // New faces should be:
        // Face 0: (3, 2, 0)
        // Face 1: (2, 3, 1)
        // Actually the way I implemented it:
        // F0: e0(3->2), e2(2->0), oe1(0->3)
        // F1: oe0(2->3), oe2(3->1), e1(1->2)
        
        // Check face 0
        val f0Idx = edge.face
        val f0Edges = mutableListOf<Int>()
        var curr = dcel.faces[f0Idx].edge
        repeat(3) {
            f0Edges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        }
        assertEquals(dcel.faces[f0Idx].edge, curr) // Circular
        
        val f0Vertices = f0Edges.map { dcel.halfEdges[it].vertex }
        // The order might depend on which edge faces[f0Idx].edge points to, 
        // but it should contain 3, 2, 0
        assertEquals(3, f0Vertices.size)
        assertTrue(f0Vertices.contains(3))
        assertTrue(f0Vertices.contains(2))
        assertTrue(f0Vertices.contains(0))

        // Check face 1
        val oeIdx = edge.otherEdge
        val f1Idx = dcel.halfEdges[oeIdx].face
        val f1Edges = mutableListOf<Int>()
        curr = dcel.faces[f1Idx].edge
        repeat(3) {
            f1Edges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        }
        assertEquals(dcel.faces[f1Idx].edge, curr) // Circular

        val f1Vertices = f1Edges.map { dcel.halfEdges[it].vertex }
        assertEquals(3, f1Vertices.size)
        assertTrue(f1Vertices.contains(2))
        assertTrue(f1Vertices.contains(3))
        assertTrue(f1Vertices.contains(1))
        
        // Verify edge (2, 3) connectivity
        assertEquals(3, dcel.halfEdges[edgeIdx].vertex)
        assertEquals(2, dcel.halfEdges[dcel.halfEdges[edgeIdx].nextEdge].vertex)
        
        assertEquals(2, dcel.halfEdges[oeIdx].vertex)
        assertEquals(3, dcel.halfEdges[dcel.halfEdges[oeIdx].nextEdge].vertex)
    }
}