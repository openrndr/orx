package org.openrndr.extra.shapes.eigenmodes

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShapeEigenModesTest {

    @Test
    fun extractsRequestedNumberOfModesForARectangle() {
        val shape = Rectangle(0.0, 0.0, 100.0, 60.0).shape
        val modes = shape.extractEigenModes(count = 4, resolution = 32)

        assertEquals(4, modes.size)
        assertEquals(1.0, modes.first().frequencyRatio, 1e-9)
        for (i in 1 until modes.size) {
            assertTrue(modes[i].eigenvalue >= modes[i - 1].eigenvalue - 1e-6, "eigenvalues must be ascending")
            assertTrue(modes[i].frequencyRatio >= modes[i - 1].frequencyRatio - 1e-6)
        }
    }

    @Test
    fun fundamentalModePeaksNearTheCenterOfARectangle() {
        val shape = Rectangle(0.0, 0.0, 80.0, 80.0).shape
        val fundamental = shape.extractEigenModes(count = 1, resolution = 32).first()

        val center = fundamental.value(Vector2(40.0, 40.0))
        val nearCorner = fundamental.value(Vector2(4.0, 4.0))
        assertTrue(
            abs(center) > abs(nearCorner),
            "fundamental mode should have larger magnitude near the center ($center) than near a corner ($nearCorner)"
        )
    }

    @Test
    fun worksForNonRectangularShapes() {
        val shape = Circle(0.0, 0.0, 50.0).shape
        val modes = shape.extractEigenModes(count = 3, resolution = 40)

        assertEquals(3, modes.size)
        val center = modes.first().value(Vector2.ZERO)
        assertTrue(abs(center) > 0.0)
    }

    @Test
    fun sampleOutsideBoundsIsClamped() {
        val shape = Rectangle(0.0, 0.0, 50.0, 50.0).shape
        val mode = shape.extractEigenModes(count = 1, resolution = 24).first()

        val atEdge = mode.value(Vector2(0.0, 0.0))
        val farOutside = mode.value(Vector2(-1000.0, -1000.0))
        assertEquals(atEdge, farOutside, 1e-9)
    }

    @Test
    fun rejectsShapeWithEmptyBounds() {
        val shape = Rectangle(0.0, 0.0, 0.0, 0.0).shape
        assertFailsWith<IllegalArgumentException> {
            shape.extractEigenModes(count = 1)
        }
    }

    @Test
    fun neumannModeZeroIsTheTrivialConstantMode() {
        val shape = Rectangle(0.0, 0.0, 100.0, 60.0).shape
        val modes = shape.extractEigenModes(
            count = 3,
            resolution = 32,
            boundaryCondition = EigenModeBoundaryCondition.NEUMANN
        )

        assertEquals(3, modes.size)
        assertEquals(0.0, modes[0].frequencyRatio, 1e-9)
        assertEquals(1.0, modes[1].frequencyRatio, 1e-6)

        val center = modes[0].value(Vector2(50.0, 30.0))
        val nearCorner = modes[0].value(Vector2(4.0, 4.0))
        assertEquals(center, nearCorner, 1e-6)
    }

    @Test
    fun boundaryEigenModesExtractsRequestedCount() {
        val contour = RectifiedContour(Circle(0.0, 0.0, 50.0).contour)
        val modes = contour.extractBoundaryEigenModes(count = 4, resolution = 40)

        assertEquals(4, modes.size)
        assertEquals(0.0, modes[0].frequencyRatio, 1e-9)
        assertEquals(1.0, modes[1].frequencyRatio, 1e-6)
        for (i in 2 until modes.size) {
            assertTrue(modes[i].frequencyRatio >= modes[i - 1].frequencyRatio - 1e-6)
        }
    }

    @Test
    fun boundaryEigenModeZeroIsExactlyConstant() {
        // regularStar is a non-convex, sharp-cornered outline -- the least "circle-like" shape
        // handy here -- to make sure mode 0's flatness doesn't depend on boundary smoothness.
        val contour = RectifiedContour(org.openrndr.extra.shapes.primitives.regularStar(5, 30.0, 70.0))
        val mode = contour.extractBoundaryEigenModes(count = 1, resolution = 50).first()

        val first = mode.values.first()
        for (v in mode.values) assertEquals(first, v, 1e-12)
    }

    @Test
    fun boundaryEigenModeValueWrapsAndMatchesSamples() {
        val contour = RectifiedContour(Circle(0.0, 0.0, 50.0).contour)
        val mode = contour.extractBoundaryEigenModes(count = 3, resolution = 20).last()

        for (i in 0 until mode.n) {
            assertEquals(mode.values[i], mode.value(i.toDouble()), 1e-9)
        }
        assertEquals(mode.value(0.0), mode.value(mode.n.toDouble()), 1e-9)
    }

    @Test
    fun rejectsOpenContourForBoundaryEigenModes() {
        val contour = RectifiedContour(org.openrndr.shape.LineSegment(Vector2.ZERO, Vector2(10.0, 0.0)).contour)
        assertFailsWith<IllegalArgumentException> {
            contour.extractBoundaryEigenModes(count = 1)
        }
    }
}
