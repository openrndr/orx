package org.openrndr.extra.math.meanvaluecoordinates

import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class TestMeanValueCoordinates {
    val e = 1E-6

    @Test
    fun testPartitionOfUnity() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(1.0, 0.0),
            Vector2(1.0, 1.0),
            Vector2(0.0, 1.0)
        )
        val point = Vector2(0.5, 0.5)
        val weights = findMVCWeights(points, point)
        assertEquals(points.size, weights.size)
        assertEquals(1.0, weights.sum(), e)
    }

    @Test
    fun testLinearPrecision() {
        val points = listOf(
            Vector2(100.0, 100.0),
            Vector2(200.0, 100.0),
            Vector2(250.0, 200.0),
            Vector2(150.0, 300.0),
            Vector2(50.0, 200.0)
        )
        val targetPoint = Vector2(150.0, 180.0)
        val weights = findMVCWeights(points, targetPoint)
        
        var reconstructed = Vector2.ZERO
        for (i in points.indices) {
            reconstructed += points[i] * weights[i]
        }
        
        assertTrue(abs(reconstructed.x - targetPoint.x) < e, "Expected ${targetPoint.x} but got ${reconstructed.x}")
        assertTrue(abs(reconstructed.y - targetPoint.y) < e, "Expected ${targetPoint.y} but got ${reconstructed.y}")
    }

    @Test
    fun testVertexProperty() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(1.0, 0.0),
            Vector2(1.0, 1.0),
            Vector2(0.0, 1.0)
        )
        for (i in points.indices) {
            val weights = findMVCWeights(points, points[i])
            for (j in weights.indices) {
                if (i == j) {
                    assertEquals(1.0, weights[j], e)
                } else {
                    assertEquals(0.0, weights[j], e)
                }
            }
        }
    }
    
    @Test
    fun testStarShaped() {
        // A star shaped polygon that is not convex
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(2.0, 0.0),
            Vector2(1.0, 1.0), // Concavity
            Vector2(2.0, 2.0),
            Vector2(0.0, 2.0)
        )
        val targetPoint = Vector2(0.5, 1.0) // This point should see all vertices
        val weights = findMVCWeights(points, targetPoint)
        
        assertEquals(1.0, weights.sum(), e)
        
        var reconstructed = Vector2.ZERO
        for (i in points.indices) {
            reconstructed += points[i] * weights[i]
        }
        assertTrue(abs(reconstructed.x - targetPoint.x) < e)
        assertTrue(abs(reconstructed.y - targetPoint.y) < e)
    }

    @Test
    fun testEdgeProperty() {
        val points = listOf(
            Vector2(0.0, 0.0),
            Vector2(2.0, 0.0),
            Vector2(2.0, 2.0),
            Vector2(0.0, 2.0)
        )
        // Point on edge between (0,0) and (2,0)
        val targetPoint = Vector2(1.0, 0.0)
        val weights = findMVCWeights(points, targetPoint)

        assertEquals(1.0, weights.sum(), e)
        // Should only have non-zero weights for points[0] and points[1]
        // Since it's exactly in the middle, weights should be 0.5 each
        assertEquals(0.5, weights[0], e)
        assertEquals(0.5, weights[1], e)
        assertEquals(0.0, weights[2], e)
        assertEquals(0.0, weights[3], e)
    }
}
