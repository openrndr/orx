package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPolygonSignedDistance {
    @Test
    fun testSignedDistance() {
        val poly = Polygon2D(listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        ))

        // Outside
        assertEquals(10.0, poly.signedDistance(Vector2(-10.0, 50.0)), 1e-6)
        assertEquals(10.0, poly.signedDistance(Vector2(50.0, -10.0)), 1e-6)
        assertEquals(10.0, poly.signedDistance(Vector2(110.0, 50.0)), 1e-6)
        assertEquals(10.0, poly.signedDistance(Vector2(50.0, 110.0)), 1e-6)

        // Inside
        assertEquals(-10.0, poly.signedDistance(Vector2(10.0, 50.0)), 1e-6)
        assertEquals(-10.0, poly.signedDistance(Vector2(50.0, 10.0)), 1e-6)
        assertEquals(-10.0, poly.signedDistance(Vector2(90.0, 50.0)), 1e-6)
        assertEquals(-10.0, poly.signedDistance(Vector2(50.0, 90.0)), 1e-6)

        // On edge
        assertEquals(0.0, poly.signedDistance(Vector2(0.0, 50.0)), 1e-6)
        assertEquals(0.0, poly.signedDistance(Vector2(50.0, 0.0)), 1e-6)

        // Corners
        assertEquals(sqrt(200.0), poly.signedDistance(Vector2(-10.0, -10.0)), 1e-6)
        assertEquals(-10.0, poly.signedDistance(Vector2(10.0, 10.0)), 1e-6)
    }

    private fun sqrt(d: Double) = kotlin.math.sqrt(d)
}
