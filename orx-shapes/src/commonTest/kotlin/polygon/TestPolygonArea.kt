package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPolygonArea {
    @Test
    fun testRectangleArea() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 50.0),
            Vector2(0.0, 50.0)
        )
        val polygon = Polygon2D(points)
        assertEquals(5000.0, polygon.area(), 1e-10)
    }

    @Test
    fun testTriangleArea() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(50.0, 100.0)
        )
        val polygon = Polygon2D(points)
        assertEquals(5000.0, polygon.area(), 1e-10)
    }

    @Test
    fun testConcavePolygonArea() {
        // Same polygon as in TestConcavePolygon.kt
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(50.0, 50.0),
            Vector2(0.0, 100.0)
        )
        // This is a 100x100 square minus a triangle with base 100 (top edges) and height 50 (middle notch)
        // Wait, let's re-calculate.
        // (0,0), (100,0), (100,100), (50,50), (0,100)
        // Area = 100*100 (outer square) - area of triangle (0,100), (100,100), (50,50)
        // Triangle area = 0.5 * base * height = 0.5 * 100 * 50 = 2500
        // Total area = 10000 - 2500 = 7500
        val polygon = Polygon2D(points)
        assertEquals(7500.0, polygon.area(), 1e-10)
    }

    @Test
    fun testWindingOrder() {
        val ccwPoints = listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        )
        val cwPoints = ccwPoints.reversed()

        assertEquals(10000.0, Polygon2D(ccwPoints).area(), 1e-10)
        assertEquals(10000.0, Polygon2D(cwPoints).area(), 1e-10)
    }

    @Test
    fun testComplexPolygonArea() {
        val outer = Polygon2D(listOf(
            Vector2(0.0, 0.0),
            Vector2(100.0, 0.0),
            Vector2(100.0, 100.0),
            Vector2(0.0, 100.0)
        ))
        val hole = Polygon2D(listOf(
            Vector2(25.0, 25.0),
            Vector2(75.0, 25.0),
            Vector2(75.0, 75.0),
            Vector2(25.0, 75.0)
        ))
        val complex = ComplexPolygon2D(outer, listOf(hole))
        // 10000 - 2500 = 7500
        assertEquals(7500.0, complex.area(), 1e-10)
    }
}
