package org.openrndr.extra.mesh.generate

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGrid {
    @Test
    fun testGridMesh() {
        val bounds = Rectangle(10.0, 20.0, 100.0, 200.0)
        val columns = 10
        val rows = 5
        val mesh = gridMesh(bounds, columns, rows)

        assertEquals((columns + 1) * (rows + 1), mesh.vertexData.positions.size)
        assertEquals(columns * rows, mesh.polygons.size)

        // Check first vertex
        assertEquals(Vector3(10.0, 20.0, 0.0), mesh.vertexData.positions[0])
        assertEquals(Vector2(0.0, 0.0), mesh.vertexData.textureCoords[0])

        // Check last vertex
        assertEquals(Vector3(110.0, 220.0, 0.0), mesh.vertexData.positions.last())
        assertEquals(Vector2(1.0, 1.0), mesh.vertexData.textureCoords.last())

        // Check first polygon indices
        val p0 = mesh.polygons[0]
        assertEquals(listOf(0, 1, columns + 2, columns + 1), p0.positions)
        
        // Check normal
        assertEquals(Vector3.UNIT_Z, mesh.vertexData.normals[0])
    }
}
