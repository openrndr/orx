package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestEdgeSplitAt {
    @Test
    fun testEdgeSplitAtBoundary() {
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
        
        // Split at t=0.75
        val newEdgeIdx = dcel.edgeSplitAt(edgeIdx, 0.75)
        
        // New vertex should be at 0.75 between (0,0,0) and (2,0,0) -> (1.5, 0, 0)
        val newVertexIdx = dcel.halfEdges[newEdgeIdx].vertex
        assertEquals(Vector3(1.5, 0.0, 0.0), dcel.vertices[newVertexIdx].position)
        
        // Face 0 should now have 4 edges
        val f0Edges = mutableListOf<Int>()
        val startEdgeIdx = dcel.faces[0].edge
        var curr = startEdgeIdx
        do {
            f0Edges.add(curr)
            curr = dcel.halfEdges[curr].nextEdge
        } while (curr != startEdgeIdx)
        
        assertEquals(4, f0Edges.size)
        
        // Check vertex sequence: should be 0 -> newVertex -> 1 -> 2
        val f0Vertices = f0Edges.map { dcel.halfEdges[it].vertex }
        val idx0 = f0Vertices.indexOf(0)
        assertEquals(newVertexIdx, f0Vertices[(idx0 + 1) % 4])
        assertEquals(1, f0Vertices[(idx0 + 2) % 4])
        assertEquals(2, f0Vertices[(idx0 + 3) % 4])
    }

    @Test
    fun testEdgeSplitAtInterior() {
        // Two triangles sharing edge (0, 1)
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(10.0, 0.0, 0.0), // 1
                    Vector3(5.0, 5.0, 0.0), // 2
                    Vector3(5.0, -5.0, 0.0) // 3
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
        
        // Split at t=0.2
        val newEIdx = dcel.edgeSplitAt(e0Idx, 0.2)
        val newVertexIdx = dcel.halfEdges[newEIdx].vertex
        
        assertEquals(Vector3(2.0, 0.0, 0.0), dcel.vertices[newVertexIdx].position)
        
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
        
        // Check otherEdge links
        val oe0 = dcel.halfEdges[oeIdx]
        val newOeIdx = e0.otherEdge
        
        assertEquals(newOeIdx, e0.otherEdge)
        assertEquals(e0Idx, dcel.halfEdges[newOeIdx].otherEdge)
        
        assertEquals(oeIdx, dcel.halfEdges[newEIdx].otherEdge)
        assertEquals(newEIdx, oe0.otherEdge)
    }
    @Test
    fun testEdgeSplitAtAttributes() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0
                    Vector3(10.0, 0.0, 0.0), // 1
                    Vector3(5.0, 5.0, 0.0) // 2
                ),
                colors = listOf(
                    org.openrndr.color.ColorRGBa.RED, // 0
                    org.openrndr.color.ColorRGBa.BLUE, // 1
                    org.openrndr.color.ColorRGBa.GREEN // 2
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2),
                    colors = listOf(0, 1, 2),
                    textureCoords = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        val edgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 0 && dcel.halfEdges[it.nextEdge].vertex == 1 }
        
        // Split at 0.5. Red + Blue at 0.5 should be (0.5, 0, 0.5, 1) in linear
        dcel.edgeSplitAt(edgeIdx, 0.5)
        
        // New vertex is now vertices[3]. Let's find the edge starting at it.
        val newEdgeIdx = dcel.halfEdges.indexOfFirst { it.vertex == 3 }
        val colorIdx = dcel.halfEdges[newEdgeIdx].attributes[0]
        assertNotEquals(-1, colorIdx)
        
        val color = dcel.colors[colorIdx]
        val expected = (org.openrndr.color.ColorRGBa.RED.toLinear() * 0.5 + org.openrndr.color.ColorRGBa.BLUE.toLinear() * 0.5).toLinear()
        
        assertEquals(expected.r, color.r, 1e-6)
        assertEquals(expected.g, color.g, 1e-6)
        assertEquals(expected.b, color.b, 1e-6)
    }
}
