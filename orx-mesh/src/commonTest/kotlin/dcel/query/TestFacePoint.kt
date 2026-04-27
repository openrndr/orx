package org.openrndr.extra.mesh.dcel.query

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestFacePoint {
    @Test
    fun testFacePointInterpolation() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(0.0, 1.0, 0.0)
                ),
                colors = listOf(
                    ColorRGBa.RED,
                    ColorRGBa.GREEN,
                    ColorRGBa.BLUE,
                    ColorRGBa.WHITE
                ),
                textureCoords = listOf(
                    Vector2(0.0, 0.0),
                    Vector2(1.0, 0.0),
                    Vector2(1.0, 1.0),
                    Vector2(0.0, 1.0)
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2, 3),
                    textureCoords = listOf(0, 1, 2, 3),
                    colors = listOf(0, 1, 2, 3),
                    normals = emptyList(),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        
        // Center point
        val centerPos = Vector3(0.5, 0.5, 0.0)
        val point = dcel.facePoint(0, centerPos)
        
        assertEquals(centerPos, point.position)
        assertNotNull(point.color)
        assertNotNull(point.textureCoordinate)
        
        // At center of a square, MVC weights should be equal (0.25 each)
        // Average of RED, GREEN, BLUE, WHITE
        val expectedColor = (ColorRGBa.RED.toLinear() + ColorRGBa.GREEN.toLinear() + ColorRGBa.BLUE.toLinear() + ColorRGBa.WHITE.toLinear()) * 0.25
        assertEquals(expectedColor.r, point.color!!.toLinear().r, 1e-6)
        assertEquals(expectedColor.g, point.color!!.toLinear().g, 1e-6)
        assertEquals(expectedColor.b, point.color!!.toLinear().b, 1e-6)
        
        assertEquals(Vector2(0.5, 0.5), point.textureCoordinate)
    }

    @Test
    fun testFacePointAttributesIndices() {
        val meshData = MeshData(
            VertexData(
                positions = listOf(
                    Vector3(0.0, 0.0, 0.0),
                    Vector3(1.0, 0.0, 0.0),
                    Vector3(1.0, 1.0, 0.0)
                ),
                normals = listOf(
                    Vector3.UNIT_Z
                )
            ),
            polygons = listOf(
                IndexedPolygon(
                    positions = listOf(0, 1, 2),
                    textureCoords = emptyList(),
                    colors = emptyList(),
                    normals = listOf(0, 0, 0),
                    tangents = emptyList(),
                    bitangents = emptyList()
                )
            )
        )

        val dcel = meshData.toDcel()
        val point = dcel.facePoint(0, Vector3(0.33, 0.33, 0.0))
        
        // Normal index should be 0 because all vertices share it
        assertEquals(0, point.attributes[2]) // NORMAL index is 2
    }
}
