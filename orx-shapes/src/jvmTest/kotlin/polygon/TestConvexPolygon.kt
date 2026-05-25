package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestConvexPolygon {
    @Test
    fun testIsPointInConvexPolygonCCW() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        )
        // Inside
        assertTrue(isPointInConvexPolygon(points, Vector2(50.0, 50.0)))
        // On edge
        assertTrue(isPointInConvexPolygon(points, Vector2(50.0, 0.0)))
        // On vertex
        assertTrue(isPointInConvexPolygon(points, Vector2(0.0, 0.0)))
        // Outside
        assertFalse(isPointInConvexPolygon(points, Vector2(-1.0, 50.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(101.0, 50.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(50.0, -1.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(50.0, 101.0)))
    }

    @Test
    fun testIsPointInConvexPolygonCW() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(0.0, 100.0),
            Vector2(100.0, 100.0),
            Vector2(100.0, 0.0)
        )
        // Inside
        assertTrue(isPointInConvexPolygon(points, Vector2(50.0, 50.0)))
        // On edge
        assertTrue(isPointInConvexPolygon(points, Vector2(50.0, 0.0)))
        // On vertex
        assertTrue(isPointInConvexPolygon(points, Vector2(0.0, 0.0)))
        // Outside
        assertFalse(isPointInConvexPolygon(points, Vector2(-1.0, 50.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(101.0, 50.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(50.0, -1.0)))
        assertFalse(isPointInConvexPolygon(points, Vector2(50.0, 101.0)))
    }

    @Test
    fun testIsConvexPolygon() {
        val ccwSquare = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        )
        assertTrue(isConvexPolygon(ccwSquare))

        val cwSquare = listOf(
            Vector2(0.0, 0.0),
            Vector2(0.0, 100.0),
            Vector2(100.0, 100.0),
            Vector2(100.0, 0.0)
        )
        assertTrue(isConvexPolygon(cwSquare))

        val concave = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(50.0, 50.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        )
        assertFalse(isConvexPolygon(concave))
    }

    @Test
    fun testDegenerate() {
        val point = listOf(Vector2(0.0, 0.0))
        assertTrue(isPointInConvexPolygon(point, Vector2(0.0, 0.0)))
        assertFalse(isPointInConvexPolygon(point, Vector2(1.0, 1.0)))

        val line = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0))
        assertTrue(isPointInConvexPolygon(line, Vector2(50.0, 0.0)))
        assertTrue(isPointInConvexPolygon(line, Vector2(0.0, 0.0)))
        assertTrue(isPointInConvexPolygon(line, Vector2(100.0, 0.0)))
        assertFalse(isPointInConvexPolygon(line, Vector2(50.0, 1.0)))
        assertFalse(isPointInConvexPolygon(line, Vector2(-1.0, 0.0)))
        assertFalse(isPointInConvexPolygon(line, Vector2(101.0, 0.0)))
    }
    @Test
    fun testIsPointInConvexPolygon3() {
        val points = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(100.0, 0.0, 0.0),
            Vector3(100.0, 100.0, 0.0),
            Vector3(0.0, 100.0, 0.0)
        )
        // Inside
        assertTrue(isPointInConvexPolygon(points, Vector3(50.0, 50.0, 0.0)))
        // On edge
        assertTrue(isPointInConvexPolygon(points, Vector3(50.0, 0.0, 0.0)))
        // On vertex
        assertTrue(isPointInConvexPolygon(points, Vector3(0.0, 0.0, 0.0)))
        // Outside (same plane)
        assertFalse(isPointInConvexPolygon(points, Vector3(-1.0, 50.0, 0.0)))
        // Outside (off plane)
        assertFalse(isPointInConvexPolygon(points, Vector3(50.0, 50.0, 1.0)))

        // Tilted polygon
        val tiltedPoints = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(100.0, 0.0, 100.0),
            Vector3(100.0, 100.0, 100.0),
            Vector3(0.0, 100.0, 0.0)
        )
        // Inside
        assertTrue(isPointInConvexPolygon(tiltedPoints, Vector3(50.0, 50.0, 50.0)))
        // Outside
        assertFalse(isPointInConvexPolygon(tiltedPoints, Vector3(50.0, 50.0, 51.0)))
    }

    @Test
    fun testDegenerate3() {
        val point = listOf(Vector3(0.0, 0.0, 0.0))
        assertTrue(isPointInConvexPolygon(point, Vector3(0.0, 0.0, 0.0)))
        assertFalse(isPointInConvexPolygon(point, Vector3(1.0, 1.0, 0.0)))

        val line = listOf(Vector3(0.0, 0.0, 0.0), Vector3(100.0, 0.0, 0.0))
        assertTrue(isPointInConvexPolygon(line, Vector3(50.0, 0.0, 0.0)))
        assertFalse(isPointInConvexPolygon(line, Vector3(50.0, 1.0, 0.0)))
        
        val collinear = listOf(Vector3(0.0, 0.0, 0.0), Vector3(50.0, 0.0, 0.0), Vector3(100.0, 0.0, 0.0))
        assertTrue(isPointInConvexPolygon(collinear, Vector3(25.0, 0.0, 0.0)))
        assertFalse(isPointInConvexPolygon(collinear, Vector3(25.0, 1.0, 0.0)))
    }
}
