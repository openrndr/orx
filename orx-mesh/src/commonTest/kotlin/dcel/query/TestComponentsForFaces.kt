package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestComponentsForFaces {
    @Test
    fun testComponentsForFaces() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0),
                    
                    Vector3(2.0, 0.0, 0.0),
                    Vector3(3.0, 0.0, 0.0),
                    Vector3(3.0, 1.0, 0.0),
                    Vector3(2.0, 1.0, 0.0),

                    Vector3(0.0, 2.0, 0.0),
                    Vector3(1.0, 2.0, 0.0),
                    Vector3(1.0, 3.0, 0.0),
                    Vector3(0.0, 3.0, 0.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(listOf(0, 1, 2, 3), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 0
                IndexedPolygon(listOf(1, 4, 7, 2), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 1, adjacent to 0
                IndexedPolygon(listOf(4, 5, 6, 7), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), // Face 2, adjacent to 1
                IndexedPolygon(listOf(8, 9, 10, 11), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()) // Face 3, isolated
            )
        )

        val dcel = meshData.toDcel()
        
        // Test all faces
        val componentsAll = dcel.componentsForFaces(listOf(0, 1, 2, 3))
        assertEquals(2, componentsAll.size)
        assertTrue(componentsAll.any { it.toSet() == setOf(0, 1, 2) })
        assertTrue(componentsAll.any { it.toSet() == setOf(3) })

        // Test subset of faces (disconnected)
        val componentsSubset = dcel.componentsForFaces(listOf(0, 2, 3))
        assertEquals(3, componentsSubset.size)
        assertTrue(componentsSubset.any { it.toSet() == setOf(0) })
        assertTrue(componentsSubset.any { it.toSet() == setOf(2) })
        assertTrue(componentsSubset.any { it.toSet() == setOf(3) })
        
        // Test subset of faces (connected)
        val componentsConnectedSubset = dcel.componentsForFaces(listOf(0, 1))
        assertEquals(1, componentsConnectedSubset.size)
        assertTrue(componentsConnectedSubset.any { it.toSet() == setOf(0, 1) })
    }
}
