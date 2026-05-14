package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestConcavePolygon {
    @Test
    fun testPointInConcavePolygon() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(50.0, 50.0),
            Vector2(0.0, 100.0)
        )
        val polygon = Polygon2D(points)

        // Inside
        assertTrue(polygon.isPointInConcavePolygon(Vector2(50.0, 25.0)))
        assertTrue(polygon.isPointInConcavePolygon(Vector2(20.0, 70.0)))
        assertTrue(polygon.isPointInConcavePolygon(Vector2(80.0, 70.0)))

        // Outside
        assertFalse(polygon.isPointInConcavePolygon(Vector2(50.0, 75.0))) // In the concave notch
        assertFalse(polygon.isPointInConcavePolygon(Vector2(-10.0, 50.0)))
        assertFalse(polygon.isPointInConcavePolygon(Vector2(110.0, 50.0)))
        assertFalse(polygon.isPointInConcavePolygon(Vector2(50.0, -10.0)))
        assertFalse(polygon.isPointInConcavePolygon(Vector2(50.0, 110.0)))

        // Boundary (behavior depends on implementation, currently it might be inconsistent)
        // point (25, 75) is on the edge (50,50) -> (0,100).
        // My implementation gives FALSE for point.x < intersectX when point.x == intersectX.
        assertFalse(polygon.isPointInConcavePolygon(Vector2(25.0, 75.0)))
    }
}
