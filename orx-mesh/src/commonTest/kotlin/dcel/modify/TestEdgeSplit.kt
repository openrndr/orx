package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestEdgeSplit {
    @Test
    fun testEdgeSplitBoundary() {
        // Single triangle (0, 1, 2)
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(1.0, 2.0, 0.0)  // 2
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
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Find edge (0, 1)
        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 0 && dcel.halfEdges[it.nextEdge].vertex == 1 }
        assertNotEquals(-1, edgeIdx)
        val edge = dcel.halfEdges[edgeIdx]
        
        val newVertexIdx = dcel.edgeSplit(edge)
        
        // 1. New vertex should be at (1, 0)
        assertEquals(Vector3(1.0, 0.0, 0.0), dcel.vertices[newVertexIdx].position)
        
        // 2. Face 0 should now have 4 edges
        val f0Edges = mutableListOf<Int>()
        val startEdgeIdx = dcel.faces[0].edge
        var curr = startEdgeIdx
        do {
            f0Edges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        } while (curr != startEdgeIdx)
        
        assertEquals(4, f0Edges.size)
        
        // 3. Check vertex sequence: should be 0 -> newVertex -> 1 -> 2
        val f0Vertices = f0Edges.map { dcel.halfEdges[it].vertex }
        // Find position of 0 in list
        val idx0 = f0Vertices.indexOf(0)
        assertEquals(newVertexIdx, f0Vertices[(idx0 + 1) % 4])
        assertEquals(1, f0Vertices[(idx0 + 2) % 4])
        assertEquals(2, f0Vertices[(idx0 + 3) % 4])
    }

    @Test
    fun testEdgeSplitInterior() {
        // Two triangles sharing edge (0, 1)
        // Face 0: (0, 1, 2)
        // Face 1: (1, 0, 3)
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(2.0, 0.0, 0.0), // 1
                    Vector3(1.0, 1.0, 0.0), // 2
                    Vector3(1.0, -1.0, 0.0) // 3
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
        
        // Find edge (0, 1) in face 0
        val e0Idx = dcel.halfEdges.indexOfFirst { it.vertex == 0 && dcel.halfEdges[it.nextEdge].vertex == 1 }
        val e0 = dcel.halfEdges[e0Idx]
        val oeIdx = e0.otherEdge
        assertNotEquals(-1, oeIdx)
        
        val newVertexIdx = dcel.edgeSplit(e0)
        
        // Both faces should have 4 edges
        fun countEdges(faceIdx: Int): Int {
            val start = dcel.faces[faceIdx].edge
            var curr = start
            var count = 0
            do {
                count++
                curr = dcel.halfEdges[curr].nextEdge
            } while (curr != start)
            return count
        }
        
        assertEquals(4, countEdges(0))
        assertEquals(4, countEdges(1))
        
        // Check connectivity of new edges
        // e0 was 0->1, now e0: 0->new, e1: new->1
        // oe was 1->0, now oe: 1->new, oenew: new->0
        
        val e1Idx = e0.nextEdge
        val oe0 = dcel.halfEdges[oeIdx]
        val oe1Idx = oe0.nextEdge
        
        assertEquals(0, e0.vertex)
        assertEquals(newVertexIdx, dcel.halfEdges[e1Idx].vertex)
        
        assertEquals(1, oe0.vertex)
        assertEquals(newVertexIdx, dcel.halfEdges[oe1Idx].vertex)
        
        // Check otherEdge links
        assertEquals(oe1Idx, e0.otherEdge)
        assertEquals(e0Idx, dcel.halfEdges[oe1Idx].otherEdge)
        
        assertEquals(oeIdx, dcel.halfEdges[e1Idx].otherEdge)
        assertEquals(e1Idx, oe0.otherEdge)
    }
}
