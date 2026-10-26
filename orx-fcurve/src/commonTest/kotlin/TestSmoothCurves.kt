import org.openrndr.extra.fcurve.FCurve
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSmoothCurves {
    private fun Segment2D.startDirection(): Vector2 =
        ((if (linear) end else control.first()) - start).normalized

    private fun Segment2D.endDirection(): Vector2 =
        (end - (if (linear) start else control.last())).normalized

    // the direction of the curve does not change where a smooth segment starts
    private fun assertSmoothJoins(fc: FCurve, from: Int = 1) {
        for (i in from until fc.segments.size) {
            val before = fc.segments[i - 1].endDirection()
            val after = fc.segments[i].startDirection()
            assertEquals(0.0, before.cross(after), 1E-9, "join $i: $before $after")
            assertTrue(before.dot(after) > 0.0, "join $i: $before $after")
        }
    }

    @Test
    fun quadraticSmoothCurvesContinueTheDirection() {
        // from the README: hold, then smooth quadratic segments
        assertSmoothJoins(fcurve("M0.5 h0.2 T0.2,0.3 T0.2,0.7 T0.2,0.3 T0.2,0.7"))
    }

    @Test
    fun cubicSmoothCurvesContinueTheDirection() {
        assertSmoothJoins(fcurve("M0.5 h0.2 S0.2,0.0,0.2,0.5 S0.2,0.0,0.2,0.5 S0.2,0.0,0.2,0.5"))
    }

    @Test
    fun smoothCurvesContinueALine() {
        for (d in listOf("M0 L1,1 T1,0", "M0 L1,1 S0.5,0,1,0")) {
            val fc = fcurve(d)
            assertSmoothJoins(fc)
            // the first control point is not on the cursor, a smooth curve after a line is not a line itself
            assertTrue(!fc.segments[1].linear && fc.segments[1].control.first() != fc.segments[1].start, d)
        }
    }

    @Test
    fun smoothCurvesAfterCurves() {
        assertSmoothJoins(fcurve("M0 C0.3,1,0.6,1,1,0.5 T1,0 S0.5,1,1,0"))
        assertSmoothJoins(fcurve("M0 Q0.5,1,1,0.5 T1,0 T1,1"))
    }

    @Test
    fun smoothCurvesAsFirstSegment() {
        val s = fcurve("M0.2 S0.5,1,1,0")
        val segment = s.segments.single()
        assertEquals(segment.start, segment.control.first())
        assertEquals(0.2, s.value(0.0), 1E-9)
        assertEquals(0.0, s.value(1.0), 1E-9)

        val t = fcurve("M0.2 T1,0")
        assertEquals(0.2, t.value(0.0), 1E-9)
        assertEquals(0.1, t.value(0.5), 1E-9)
    }
}
