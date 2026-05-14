package org.openrndr.extra.shapes.convexdecomposition

import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class ConvexDecompositionTest {

    @Test
    fun testLargePolygon() {
        val pts = Rectangle(0.0, 0.0, 720.0, 720.0).offsetEdges(-50.0).scatter(10.0, random = Random(0)).hilbertOrder()
        val c = hobbyCurve(pts, true).shape

        val convex = c.convexDecompose(10.5)

    }

    @Test
    fun testRectangle() {
        val rect = Rectangle(0.0, 0.0, 100.0, 100.0).shape
        val convexParts = rect.convexDecompose()
        // A rectangle is already convex, but triangulation might split it.
        // Hertel-Mehlhorn should merge it back to 1 part.
        assertTrue(convexParts.size == 1, "Should be 1 part, but got ${convexParts.size}")
    }

    @Test
    fun testLShape() {
        // Create an L-shaped polygon
        // (0,0) -- (2,0)
        //   |        |
        // (0,2) -- (1,2)
        //            |
        //          (1,1) -- (2,1)
        //            |        |
        //          (2,1) ---- (2,0)
        
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(2.0, 0.0),
            Vector2(2.0, 1.0),
            Vector2(1.0, 1.0),
            Vector2(1.0, 2.0),
            Vector2(0.0, 2.0)
        )
        val shape = Shape(listOf(ShapeContour.fromPoints(points, true)))
        val convexParts = shape.convexDecompose()
        
        assertTrue(convexParts.size > 0)
        for (part in convexParts) {
            assertTrue(isConvex(part), "Part should be convex")
        }
    }

    @Test
    fun testHole() {
        val outer = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val inner = Rectangle(25.0, 25.0, 50.0, 50.0).contour.reversed
        val shape = Shape(listOf(outer, inner))
        val convexParts = shape.convexDecompose()
        
        assertTrue(convexParts.size > 0)
        for (part in convexParts) {
            assertTrue(isConvex(part), "Part should be convex")
        }
    }

    @Test
    fun testComplexShape() {
        // A shape with many potential merges to increase chance of triggering the bug
        val points = mutableListOf<Vector2>()
        val n = 20
        val radius = 100.0
        for (i in 0 until n) {
            val angle = 2.0 * PI * i / n
            val r = if (i % 2 == 0) radius else radius * 0.5
            points.add(Vector2(radius + r * cos(angle), radius + r * sin(angle)))
        }
        val shape = Shape(listOf(ShapeContour.fromPoints(points, true)))
        // This should not throw IndexOutOfBoundsException
        val convexParts = shape.convexDecompose()
        assertTrue(convexParts.isNotEmpty())
    }

    private fun isConvex(contour: ShapeContour): Boolean {
        val points = contour.adaptivePositions(0.1)
        if (points.size < 3) return true
        
        val pts = if (points.first().distanceTo(points.last()) < 1e-6) points.dropLast(1) else points
        
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            val c = pts[(i + 2) % pts.size]
            
            val cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
            // Depending on winding, it should be consistently non-negative or non-positive
            // triangulate usually returns CCW, so cross product should be >= 0
            if (cross < -1e-4) return false
        }
        return true
    }
}
