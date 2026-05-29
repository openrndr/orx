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

    @Test
    fun testIgnoreAdjacent() {
        val p1 = Polygon2D(listOf(
            Vector2(0.0, 0.0),
            Vector2(10.0, 0.0),
            Vector2(10.0, 10.0),
            Vector2(0.0, 10.0)
        ))

        // Shared edge (collinear)
        val p2 = Polygon2D(listOf(
            Vector2(10.0, 0.0),
            Vector2(20.0, 0.0),
            Vector2(20.0, 10.0),
            Vector2(10.0, 10.0)
        ))

        // Shared vertex and crossing
        val p3 = Polygon2D(listOf(
            Vector2(10.0, 10.0),
            Vector2(5.0, 5.0),
            Vector2(15.0, 5.0),
            Vector2(15.0, 20.0),
            Vector2(10.0, 20.0)
        ))

        // Partially shared edge (collinear)
        val p4 = Polygon2D(listOf(
            Vector2(10.0, 2.0),
            Vector2(20.0, 2.0),
            Vector2(20.0, 8.0),
            Vector2(10.0, 8.0)
        ))

        assertTrue(p1.intersects(p2), "p1 should intersect p2 with ignoreAdjacent=false")
        assertFalse(p1.intersects(p2, ignoreAdjacent = true), "p1 should NOT intersect p2 with ignoreAdjacent=true")

        assertTrue(p1.intersects(p3), "p1 should intersect p3 with ignoreAdjacent=false")
        assertTrue(p1.intersects(p3, ignoreAdjacent = true), "p1 should intersect p3 with ignoreAdjacent=true because it crosses elsewhere")

        assertTrue(p1.intersects(p4), "p1 should intersect p4 with ignoreAdjacent=false")
        assertFalse(p1.intersects(p4, ignoreAdjacent = true), "p1 should NOT intersect p4 with ignoreAdjacent=true")

        // T-junction
        val p5 = Polygon2D(listOf(
            Vector2(10.0, 5.0),
            Vector2(15.0, 2.0),
            Vector2(15.0, 8.0)
        ))
        assertTrue(p1.intersects(p5), "p1 should intersect p5 with ignoreAdjacent=false")
        assertFalse(p1.intersects(p5, ignoreAdjacent = true), "p1 should NOT intersect p5 with ignoreAdjacent=true")

        // Fully inside (no boundary contact)
        val p6 = Polygon2D(listOf(
            Vector2(2.0, 2.0),
            Vector2(8.0, 2.0),
            Vector2(8.0, 8.0),
            Vector2(2.0, 8.0)
        ))
        assertTrue(p1.intersects(p6), "p1 should intersect p6 with ignoreAdjacent=false")
        assertTrue(p1.intersects(p6, ignoreAdjacent = true), "p1 should intersect p6 with ignoreAdjacent=true")

        // Crossing
        val p7 = Polygon2D(listOf(
            Vector2(5.0, -2.0),
            Vector2(5.0, 12.0),
            Vector2(6.0, 12.0),
            Vector2(6.0, -2.0)
        ))
        assertTrue(p1.intersects(p7), "p1 should intersect p7 with ignoreAdjacent=false")
        assertTrue(p1.intersects(p7, ignoreAdjacent = true), "p1 should intersect p7 with ignoreAdjacent=true")
    }
}
