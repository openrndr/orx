package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFaceSetSubdivide {
    @Test
    fun testSubdivideTriangle() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(1.0, 2.0, 0.0)
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
        val newFaces = dcel.convexFaceSetSubdivide(setOf(0))

        assertEquals(4, newFaces.size)
        assertEquals(4, dcel.faces.size)
        // Original triangle has 3 vertices. Subdividing it into 4 triangles adds 3 vertices (midpoints)
        assertEquals(6, dcel.vertices.size)
    }

    @Test
    fun testSubdivideQuad() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(2.0, 2.0, 0.0),
                    Vector3(0.0, 2.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2, 3),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        val newFaces = dcel.convexFaceSetSubdivide(setOf(0))

        assertEquals(4, newFaces.size)
        assertEquals(4, dcel.faces.size)
        // Original quad: 4 vertices. 
        // 4 midpoints + 1 center = 5 new vertices.
        // Total: 4 + 5 = 9 vertices.
        assertEquals(9, dcel.vertices.size)
        
        // Verify each face is a quad
        for (fIdx in newFaces) {
            val edges = mutableListOf<Int>()
            val start = dcel.faces[fIdx].edge
            var curr = start
            do {
                edges.add(curr)
                curr = dcel.halfEdges[curr].nextEdge
            } while (curr != start)
            assertEquals(4, edges.size, "Face $fIdx should be a quad")
        }
    }

    @Test
    fun testSubdivideGrid() {
        val meshData = gridMesh(Rectangle(0.0, 0.0, 100.0, 100.0), 2, 2)
        val dcel = meshData.toDcel()

        assertEquals(4, dcel.faces.size)

        // Subdivide all faces
        val faceIds = dcel.faces.indices.toSet()
        val newFaces = dcel.convexFaceSetSubdivide(faceIds)

        assertEquals(16, newFaces.size)
        assertEquals(16, dcel.faces.size)

        // Grid 2x2 has 9 vertices.
        // After subdivision:
        // Each of the 4 quads gets 1 center vertex -> +4 vertices
        // Each edge is split.
        // Number of unique edges in 2x2 grid:
        // Horizontal: 3 * 2 = 6
        // Vertical: 2 * 3 = 6
        // Total unique edges = 12.
        // Each split adds 1 vertex -> +12 vertices.
        // Total vertices = 9 + 4 + 12 = 25.
        assertEquals(25, dcel.vertices.size)

        // Verify each face is a quad
        for (fIdx in newFaces) {
            val edges = mutableListOf<Int>()
            val start = dcel.faces[fIdx].edge
            var curr = start
            do {
                edges.add(curr)
                curr = dcel.halfEdges[curr].nextEdge
            } while (curr != start)
            assertEquals(4, edges.size, "Face $fIdx should be a quad")
        }
    }
}
