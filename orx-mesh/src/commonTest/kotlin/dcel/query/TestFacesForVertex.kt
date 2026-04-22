package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFacesForVertex {
    @Test
    fun testSingleFace() {
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
        for (i in 0..3) {
            val faces = dcel.facesForVertex(i)
            assertEquals(1, faces.size)
            assertTrue(faces.contains(dcel.faces[0]))
        }
    }

    @Test
    fun testTwoFacesSharedVertex() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(0.0, 1.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(2.0, 1.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 4, 3),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                ),
                IndexedPolygon(
                    positions = listOf(1, 2, 5, 4),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Vertices 1 and 4 are shared
        val faces1 = dcel.facesForVertex(1)
        assertEquals(2, faces1.size)
        assertTrue(faces1.contains(dcel.faces[0]))
        assertTrue(faces1.contains(dcel.faces[1]))

        val faces4 = dcel.facesForVertex(4)
        assertEquals(2, faces4.size)
        assertTrue(faces4.contains(dcel.faces[0]))
        assertTrue(faces4.contains(dcel.faces[1]))

        // Vertices 0, 3, 2, 5 are NOT shared
        assertEquals(1, dcel.facesForVertex(0).size)
        assertEquals(1, dcel.facesForVertex(3).size)
        assertEquals(1, dcel.facesForVertex(2).size)
        assertEquals(1, dcel.facesForVertex(5).size)
    }

    @Test
    fun testFourFacesSharedVertex() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0), // 0: center
                    Vector3(1.0, 0.0, 0.0), // 1: right
                    Vector3(0.0, 1.0, 0.0), // 2: top
                    Vector3(-1.0, 0.0, 0.0), // 3: left
                    Vector3(0.0, -1.0, 0.0)  // 4: bottom
                )
            ),
            polygons = listOf(
                IndexedPolygon(listOf(0, 1, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(0, 2, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(0, 3, 4), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                IndexedPolygon(listOf(0, 4, 1), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            )
        )

        val dcel = meshData.toDcel()
        val faces0 = dcel.facesForVertex(0)
        assertEquals(4, faces0.size)
        for (i in 0..3) {
            assertTrue(faces0.contains(dcel.faces[i]))
        }
    }
}
