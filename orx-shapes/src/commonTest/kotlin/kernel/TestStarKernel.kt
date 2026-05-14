package org.openrndr.extra.shapes.kernel

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class TestStarKernel {
    @Test
    fun testConvexPolygon() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(1.0, 0.0),
            Vector2(1.0, 1.0),
            Vector2(0.0, 1.0)
        )
        val kernel = findKernel(points)
        // For a convex polygon, the kernel is the polygon itself
        assertEquals(4, kernel.size)
    }

    @Test
    fun testStarPolygon() {
        // A simple star-shaped polygon (a cross-like shape)
        val points = listOf(
            Vector2(1.0, 0.0),
            Vector2(2.0, 0.0),
            Vector2(2.0, 1.0),
            Vector2(3.0, 1.0),
            Vector2(3.0, 2.0),
            Vector2(2.0, 2.0),
            Vector2(2.0, 3.0),
            Vector2(1.0, 3.0),
            Vector2(1.0, 2.0),
            Vector2(0.0, 2.0),
            Vector2(0.0, 1.0),
            Vector2(1.0, 1.0)
        )
        val kernel = findKernel(points)
        assertTrue(kernel.isNotEmpty())
        // The kernel of this plus shape should be the central 1x1 square
        // from (1,1) to (2,2)
        assertEquals(4, kernel.size)
        
        val sortedKernel = kernel.sortedBy { it.x * 10 + it.y }
        assertEquals(Vector2(1.0, 1.0), sortedKernel[0])
        assertEquals(Vector2(1.0, 2.0), sortedKernel[1])
        assertEquals(Vector2(2.0, 1.0), sortedKernel[2])
        assertEquals(Vector2(2.0, 2.0), sortedKernel[3])
    }

    @Test
    fun testNonStarPolygon() {
        // A U-shaped polygon (not star-shaped)
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(3.0, 0.0),
            Vector2(3.0, 3.0),
            Vector2(2.0, 3.0),
            Vector2(2.0, 1.0),
            Vector2(1.0, 1.0),
            Vector2(1.0, 3.0),
            Vector2(0.0, 3.0)
        )
        val kernel = findKernel(points)
        assertTrue(kernel.isEmpty())
    }
}
