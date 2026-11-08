import org.openrndr.extra.fcurve.fcurve
import org.openrndr.extra.fcurve.toFCurveString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFCurveString {
    private fun assertRoundTrips(text: String) {
        val curve = fcurve(text)
        val back = fcurve(curve.toFCurveString())
        assertEquals(curve.segments.size, back.segments.size, "segments of '$text' -> '${curve.toFCurveString()}'")
        for ((a, b) in curve.segments.zip(back.segments)) {
            assertEquals(a.control.size, b.control.size)
            assertTrue(a.start.distanceTo(b.start) < 1E-9 && a.end.distanceTo(b.end) < 1E-9, "$a != $b")
            for ((p, q) in a.control.zip(b.control)) assertTrue(p.distanceTo(q) < 1E-9, "$a != $b")
        }
    }

    @Test
    fun linesCurvesAndJumps() {
        assertRoundTrips("M0 L1,1 C0.3,1,0.6,-2,1,-2 M4 Q0.5,4,1,0 T1,2 S0.5,-1,1,-1 h2")
        assertRoundTrips("H-1.5 M2 L0.25,1E-7 L3,-0.125")
    }

    @Test
    fun emptyAndConstant() {
        assertEquals("", fcurve { }.toFCurveString())
        assertRoundTrips("M3 L0,3")
    }

    @Test
    fun longRandomCurve() {
        val r = kotlin.random.Random(3)
        val curve = fcurve {
            moveTo(0.0)
            for (i in 0 until 300) {
                if (i % 50 == 49) moveTo(r.nextDouble())
                continueTo(r.nextDouble(0.1, 0.3), r.nextDouble(-1.0, 1.0))
            }
        }
        val back = fcurve(curve.toFCurveString())
        assertEquals(curve.segments.size, back.segments.size)
        for ((a, b) in curve.segments.zip(back.segments)) {
            assertTrue(a.start.distanceTo(b.start) < 1E-9 && a.end.distanceTo(b.end) < 1E-9)
            for ((p, q) in a.control.zip(b.control)) assertTrue(p.distanceTo(q) < 1E-9)
        }
    }
}
