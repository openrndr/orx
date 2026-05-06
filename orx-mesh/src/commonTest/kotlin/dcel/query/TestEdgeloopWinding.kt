package org.openrndr.extra.mesh.dcel.query

import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.polygonToDcel
import org.openrndr.math.Vector2
import org.openrndr.shape.Winding
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEdgeloopWinding {
    @Test
    fun testClockwiseWinding() {
        val outer = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        )
        val dcel = polygonToDcel(outer, emptyList())
        // polygonToDcel seems to create edges in the order of the list.
        // (0,0) -> (100,0) -> (100,100) -> (0,100)
        // This is CLOCKWISE in a coordinate system where Y is down (typical for OPENRNDR).
        // Let's check how Winding is defined.
        assertEquals(Winding.CLOCKWISE, dcel.edgeloopWinding(0))

        assertEquals(dcel.faceToShape(0).contours[0].winding, Winding.CLOCKWISE)
    }

    @Test
    fun testCounterClockwiseWinding() {
        val outer = listOf(
            Vector2(0.0, 0.0),
            Vector2(0.0, 100.0),
            Vector2(100.0, 100.0),
            Vector2(100.0, 0.0)
        )
        val dcel = polygonToDcel(outer, emptyList())
        assertEquals(Winding.COUNTER_CLOCKWISE, dcel.edgeloopWinding(0))
    }

    @Test
    fun testNonPlanarLoop() {
        val dcel = polygonToDcel(listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        ), emptyList())
        // Manually move one vertex out of plane
        dcel.vertices[2].position = dcel.vertices[2].position.copy(z = 10.0)
        
        try {
            dcel.edgeloopWinding(0)
            kotlin.test.fail("Should have thrown IllegalArgumentException for non-planar loop")
        } catch (e: IllegalArgumentException) {
            assertEquals("edge loop is not planar", e.message)
        }
    }
}
