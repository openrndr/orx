package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPolygonIntersection {
    @Test
    fun testIntersects() {
        val p1 = Polygon2D(listOf(
            Vector2(0.0, 0.0),
            Vector2(10.0, 0.0),
            Vector2(10.0, 10.0),
            Vector2(0.0, 10.0)
        ))

        val p2 = Polygon2D(listOf(
            Vector2(5.0, 5.0),
            Vector2(15.0, 5.0),
            Vector2(15.0, 15.0),
            Vector2(5.0, 15.0)
        ))

        val p3 = Polygon2D(listOf(
            Vector2(20.0, 20.0),
            Vector2(30.0, 20.0),
            Vector2(30.0, 30.0),
            Vector2(20.0, 30.0)
        ))

        val p4 = Polygon2D(listOf(
            Vector2(2.0, 2.0),
            Vector2(8.0, 2.0),
            Vector2(8.0, 8.0),
            Vector2(2.0, 8.0)
        ))

        assertTrue(p1.intersects(p2))
        assertTrue(p2.intersects(p1))

        assertFalse(p1.intersects(p3))
        assertFalse(p3.intersects(p1))

        assertTrue(p1.intersects(p4))
        assertTrue(p4.intersects(p1))
    }

    @Test
    fun testConcaveIntersects() {
        // L-shape
        val p1 = Polygon2D(listOf(
            Vector2(0.0, 0.0),
            Vector2(10.0, 0.0),
            Vector2(10.0, 2.0),
            Vector2(2.0, 2.0),
            Vector2(2.0, 10.0),
            Vector2(0.0, 10.0)
        ))

        // Box that fits in the "gap" of the L-shape
        val p2 = Polygon2D(listOf(
            Vector2(3.0, 3.0),
            Vector2(9.0, 3.0),
            Vector2(9.0, 9.0),
            Vector2(3.0, 9.0)
        ))

        // Box that overlaps the L-shape
        val p3 = Polygon2D(listOf(
            Vector2(1.0, 1.0),
            Vector2(3.0, 1.0),
            Vector2(3.0, 3.0),
            Vector2(1.0, 3.0)
        ))

        assertFalse(p1.intersects(p2))
        assertTrue(p1.intersects(p3))
    }
}
