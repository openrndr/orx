package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TestEdgeloopContains {
    @Test
    fun testSimpleSquare() {
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
        val edgeId = dcel.faces[0].edge
        
        assertTrue(dcel.edgeloopContains(edgeId, Vector3(0.5, 0.5, 0.0)))
        assertTrue(dcel.edgeloopContains(edgeId, Vector3(0.1, 0.1, 0.0)))
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(1.5, 0.5, 0.0)))
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(-0.5, 0.5, 0.0)))
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(0.5, 1.5, 0.0)))
        
        // Point on plane but outside
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(2.0, 2.0, 0.0)))
        
        // Point off plane
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(0.5, 0.5, 1.0)))
    }

    @Test
    fun testConcaveLoop() {
        // L-shape
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(2.0, 1.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(1.0, 2.0, 0.0),
                    Vector3(0.0, 2.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2, 3, 4, 5),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )
        val dcel = meshData.toDcel()
        val edgeId = dcel.faces[0].edge

        assertTrue(dcel.edgeloopContains(edgeId, Vector3(0.5, 0.5, 0.0)))
        assertTrue(dcel.edgeloopContains(edgeId, Vector3(0.5, 1.5, 0.0)))
        assertTrue(dcel.edgeloopContains(edgeId, Vector3(1.5, 0.5, 0.0)))
        
        // In the "dent" of the L-shape
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(1.5, 1.5, 0.0)))
    }

    @Test
    fun testDifferentOrientations() {
        // Square on XZ plane
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 1.0),
                    Vector3(0.0, 0.0, 1.0)
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
        val edgeId = dcel.faces[0].edge
        
        assertTrue(dcel.edgeloopContains(edgeId, Vector3(0.5, 0.0, 0.5)))
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(0.5, 0.1, 0.5)))
        assertFalse(dcel.edgeloopContains(edgeId, Vector3(1.5, 0.0, 0.5)))
    }
}
