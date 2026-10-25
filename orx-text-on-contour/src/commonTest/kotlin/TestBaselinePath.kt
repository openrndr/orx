import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.BaselinePath
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBaselinePath {
    private fun BaselinePath.position(u: Double): Vector2 = (pose(u) * Vector4.UNIT_W).xy

    @Test
    fun withoutOffsetTheBaselineIsTheContour() {
        val contour = Circle(0.0, 0.0, 100.0).contour.rectified()
        val baseline = BaselinePath(contour, 0.0)
        assertEquals(contour.contour.length, baseline.length, 1E-9)
        assertEquals(0.3, baseline.t(0.3), 1E-12)
    }

    @Test
    fun offsetOnACircle() {
        val contour = Circle(0.0, 0.0, 100.0).contour.rectified()
        for (offset in listOf(10.0, -10.0)) {
            val baseline = BaselinePath(contour, offset)
            val radius = baseline.position(0.0).length
            assertEquals(10.0, abs(radius - 100.0), 1E-3)
            // the circle is a Bézier approximation, its length differs slightly from 2 pi r
            assertEquals(2.0 * PI * radius, baseline.length, 2E-4 * baseline.length)
            // equal steps along the baseline are equal distances on the offset circle
            val steps = (0..8).map { baseline.position(it / 8.0) }
            val chords = steps.zipWithNext { a, b -> a.distanceTo(b) }
            assertTrue(chords.all { abs(it - chords.first()) < 0.05 }, "$chords")
            assertTrue(steps.all { abs(it.length - radius) < 1E-3 })
        }
        // opposite offsets end up on opposite sides of the contour
        val outer = BaselinePath(contour, 10.0).position(0.0).length
        val inner = BaselinePath(contour, -10.0).position(0.0).length
        assertEquals(20.0, abs(outer - inner), 1E-3)
    }

    @Test
    fun offsetMovesAlongTheNormal() {
        val contour = LineSegment(0.0, 0.0, 100.0, 0.0).contour.rectified()
        val baseline = BaselinePath(contour, 5.0)
        assertEquals(100.0, baseline.length, 1E-6)
        val expected = contour.position(0.5) + contour.normal(0.5) * 5.0
        assertEquals(0.0, baseline.position(0.5).distanceTo(expected), 1E-6)
        // open contours clamp positions beyond their ends
        assertEquals(1.0, baseline.t(1.5), 1E-12)
    }
}
