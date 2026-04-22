package query

import org.openrndr.extra.mesh.generate.boxMesh
import org.openrndr.extra.mesh.query.edges
import org.openrndr.extra.mesh.query.polygonsAdjacentToEdge
import org.openrndr.extra.mesh.query.polygonsAdjacentToPolygon
import org.openrndr.extra.mesh.query.polygonsAdjacentToVertex
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAdjacentPolygons {

    @Test
    fun testAdjacentPolygons() {
        val mesh = boxMesh(Vector3.ZERO, 1.0, 1.0, 1.0)
        for (i in mesh.polygons.indices) {
            val adjacent = mesh.polygonsAdjacentToPolygon(i)
            assertEquals(4, adjacent.size)
        }
    }

    @Test
    fun testAdjacentPolygons2() {
        val mesh = boxMesh(Vector3.ZERO, 1.0, 1.0, 1.0)
        for (edge in mesh.edges()) {
            val adjacent = mesh.polygonsAdjacentToEdge(edge)
            assertEquals(
                2,
                adjacent.size
            )
        }
    }

    @Test
    fun testAdjacentPolygons3() {
        val mesh = boxMesh(Vector3.ZERO, 1.0, 1.0, 1.0)
        for (i in mesh.vertexData.positions.indices) {
            val adjacent = mesh.polygonsAdjacentToVertex(i)
            assertEquals(3,
                adjacent.size
            )
        }
    }
}