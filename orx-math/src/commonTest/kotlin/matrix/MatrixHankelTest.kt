package org.openrndr.extra.math.matrix

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals

class MatrixHankelTest {

    @Test
    fun testHankelOpen() {
        val points = List(10) { Vector2(it.toDouble(), (it * 2).toDouble()) }
        val matrix = points.hankel(4, false)
        val points2 = matrix.dehankelVector2(false)
        assertEquals(points.size, points2.size)
        for (i in points.indices) {
            assertEquals(points[i].x, points2[i].x, 1e-6)
            assertEquals(points[i].y, points2[i].y, 1e-6)
        }
    }

    @Test
    fun testHankelClosed() {
        val points = List(10) { Vector2(it.toDouble(), (it * 3).toDouble()) }
        val matrix = points.hankel(4, true)
        val points2 = matrix.dehankelVector2(true)
        assertEquals(points.size, points2.size)
        for (i in points.indices) {
            assertEquals(points[i].x, points2[i].x, 1e-6)
            assertEquals(points[i].y, points2[i].y, 1e-6)
        }
    }

    @Test
    fun testHankel() {
        val points = List(10) { Vector2(it.toDouble(), it.toDouble()) }
        val matrix = points.hankel(4, false)
        val points2 = matrix.dehankelVector2()
        assertEquals(points.size, points2.size)
        for (i in points.indices) {
            assertEquals(points[i].x, points2[i].x, 1e-6)
            assertEquals(points[i].y, points2[i].y, 1e-6)
        }
    }
}