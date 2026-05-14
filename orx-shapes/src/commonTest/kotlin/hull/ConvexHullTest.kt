package org.openrndr.extra.shapes.hull

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvexHullTest {
    @Test
    fun testSquare() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(1.0, 0.0),
            Vector2(1.0, 1.0),
            Vector2(0.0, 1.0),
            Vector2(0.5, 0.5) // inner point
        )
        val hull = points.convexHull()
        assertEquals(4, hull.segments.size)
        val hullPoints = hull.segments.map { it.start }
        assertTrue(hullPoints.contains(Vector2(0.0, 0.0)))
        assertTrue(hullPoints.contains(Vector2(1.0, 0.0)))
        assertTrue(hullPoints.contains(Vector2(1.0, 1.0)))
        assertTrue(hullPoints.contains(Vector2(0.0, 1.0)))
    }

    @Test
    fun testCollinear() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(1.0, 1.0),
            Vector2(2.0, 2.0),
            Vector2(0.5, 0.5)
        )
        val hull = points.convexHull()
        // For collinear points, monotone chain might return just the endpoints or all of them depending on <= 0 vs < 0.
        // With <= 0, it should only keep endpoints.
        val hullPoints = hull.segments.map { it.start }
        assertEquals(2, hullPoints.size)
        assertTrue(hullPoints.contains(Vector2(0.0, 0.0)))
        assertTrue(hullPoints.contains(Vector2(2.0, 2.0)))
    }

    @Test
    fun testFewPoints() {
        val points = listOf(Vector2(0.0, 0.0), Vector2(1.0, 1.0))
        val hull = points.convexHull()
        assertEquals(2, hull.segments.size)
    }
    @Test
    fun testEmpty() {
        val points = emptyList<Vector2>()
        val hull = points.convexHull()
        assertTrue(hull.segments.isEmpty())
    }
}
