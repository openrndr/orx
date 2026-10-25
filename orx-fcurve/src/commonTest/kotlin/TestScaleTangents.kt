import org.openrndr.extra.fcurve.fcurve
import org.openrndr.extra.fcurve.scaleTangents
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestScaleTangents {
    private fun Segment2D.isFunctionShaped(): Boolean {
        val xs = (0..2000).map { position(it / 2000.0).x }
        return xs.zipWithNext().all { (a, b) -> b >= a - 1E-9 }
    }

    private fun assertParallel(expected: Vector2, actual: Vector2) {
        assertEquals(0.0, expected.normalized.cross(actual.normalized), 1E-9)
        assertTrue(expected.dot(actual) > 0.0)
    }

    @Test
    fun steepSegmentIsMadeFunctionShaped() {
        // a short, steep segment with long handles, the diagonal of the segment is longer than the handles
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(1.5, 0.0), Vector2(-0.5, 10.0), Vector2(1.0, 10.0))
        assertTrue(!segment.isFunctionShaped())
        val scaled = segment.scaleTangents()
        assertTrue(scaled.isFunctionShaped())
        // the tangent directions are kept
        assertParallel(segment.control[0] - segment.start, scaled.control[0] - scaled.start)
        assertParallel(segment.control[1] - segment.end, scaled.control[1] - scaled.end)
    }

    @Test
    fun handlesAreShortenedNoMoreThanNeeded() {
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(3.0, 1.0), Vector2(-2.0, -1.0), Vector2(1.0, 0.0))
        val scaled = segment.scaleTangents()
        assertTrue(scaled.isFunctionShaped())
        // at the limit the derivative along x touches zero, a little longer and the segment is no function anymore
        val longer = scaled.copy(
            control = listOf(
                scaled.start + (scaled.control[0] - scaled.start) * 1.01,
                scaled.end + (scaled.control[1] - scaled.end) * 1.01
            )
        )
        assertTrue(!longer.isFunctionShaped())
    }

    @Test
    fun functionShapedSegmentsAreNotChanged() {
        // the handles cross in x but the segment is still function shaped
        val crossing = Segment2D(Vector2(0.0, 0.0), Vector2(0.8, 1.0), Vector2(0.2, 1.0), Vector2(1.0, 0.0))
        assertTrue(crossing.isFunctionShaped())
        assertEquals(crossing, crossing.scaleTangents())

        val regular = Segment2D(Vector2(0.0, 0.0), Vector2(0.3, 5.0), Vector2(0.7, -5.0), Vector2(1.0, 0.0))
        assertEquals(regular, regular.scaleTangents())

        val linear = Segment2D(Vector2(0.0, 0.0), Vector2(1.0, 3.0))
        assertEquals(linear, linear.scaleTangents())
    }

    @Test
    fun backwardHandlesBecomeVertical() {
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(-0.5, 1.0), Vector2(1.5, 2.0), Vector2(1.0, 2.0))
        val scaled = segment.scaleTangents()
        assertTrue(scaled.isFunctionShaped())
        assertEquals(scaled.start.x, scaled.control[0].x, 1E-12)
        assertEquals(scaled.end.x, scaled.control[1].x, 1E-12)
        assertEquals(1.0, scaled.control[0].y, 1E-12)
    }

    @Test
    fun backwardHandleDoesNotShortenTheOtherHandle() {
        // the backward first handle has no length along x, the second handle fits on its own
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(-5.0, 1.0), Vector2(0.2, 2.0), Vector2(1.0, 2.0))
        val scaled = segment.scaleTangents()
        assertEquals(segment.control[1], scaled.control[1])
    }

    @Test
    fun quadraticSegments() {
        val valid = Segment2D(Vector2(0.0, 0.0), Vector2(0.5, 4.0), Vector2(1.0, 0.0))
        assertEquals(valid, valid.scaleTangents())

        val invalid = Segment2D(Vector2(0.0, 0.0), Vector2(2.0, 4.0), Vector2(1.0, 0.0))
        assertTrue(!invalid.isFunctionShaped())
        assertTrue(invalid.scaleTangents().isFunctionShaped())
    }

    @Test
    fun fcurveValuesFollowTheSegment() {
        // the steep segment from above written as an fcurve, every x value has the y value of the segment
        val fc = fcurve("M0 C1.5,0,-0.5,10,1,10")
        val segment = fc.segments.single()
        assertTrue(segment.isFunctionShaped())
        for (i in 1 until 100) {
            val p = segment.position(i / 100.0)
            assertTrue(abs(fc.value(p.x) - p.y) < 1E-6, "x=${p.x}")
        }

        val quadratic = fcurve("M0 Q2,4,1,0")
        assertTrue(quadratic.segments.single().isFunctionShaped())
    }
}
