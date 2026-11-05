package org.openrndr.extra.composition

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMarkerVertices {
    @Test
    fun openContour() {
        val v = markerVertices(ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0)), closed = false).shape)
        assertEquals(0.0, v.start!!.second, 1E-9)
        assertEquals(1, v.mid.size)
        // the bisector of right and down
        assertEquals(45.0, v.mid.single().second, 1E-9)
        assertEquals(Vector2(10.0, 10.0), v.end!!.first)
        assertEquals(90.0, v.end!!.second, 1E-9)
    }

    @Test
    fun closedContour() {
        val square = listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0))
        val v = markerVertices(ShapeContour.fromPoints(square, closed = true).shape)
        // the closing vertex coincides with the first, both bisect the closing and first segments
        assertEquals(Vector2.ZERO, v.end!!.first)
        assertEquals(-45.0, v.start!!.second, 1E-9)
        assertEquals(-45.0, v.end!!.second, 1E-9)
        assertEquals(3, v.mid.size)
    }
}
