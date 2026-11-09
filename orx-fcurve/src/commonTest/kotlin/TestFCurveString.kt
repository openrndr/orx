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

    @Test
    fun reverseKeepsTheKeysInOrderAndTheTimeSpan() {
        val curve = fcurve("M0 L1,100 L2,20 L1,60")
        val r = curve.reverse()
        // the keys, at 0: 60, 1: 20, 3: 100 and 4: 0
        assertEquals(listOf(0.0 to 60.0, 1.0 to 20.0, 3.0 to 100.0, 4.0 to 0.0),
            listOf(r.segments.first().start.let { it.x to it.y }) + r.segments.map { it.end.x to it.end.y })
        assertEquals(curve.start, r.start)
        assertEquals(curve.end, r.end)
        for (i in 0..40) {
            val t = i * 0.1
            assertEquals(curve.value(curve.end - t), r.value(curve.start + t), 1E-9)
        }
        // a curve that does not start at 0 stays where it is
        val late = fcurve("H2 M0 L1,10 C0.3,10,0.6,-5,1,-5")
        val rl = late.reverse()
        assertEquals(2.0, rl.start, 1E-12)
        assertEquals(4.0, rl.end, 1E-12)
        for (i in 0..20) {
            val t = i * 0.1
            assertEquals(late.value(late.end - t), rl.value(late.start + t), 1E-9)
        }
        assertTrue(rl.segments.zipWithNext().all { (a, b) -> a.end.x <= b.start.x + 1E-12 })
    }

    @Test
    fun negativeSpeedReversesAndScales() {
        val curve = fcurve("M0 L1,100 L2,20 L1,60")
        val c = curve.changeSpeed(-2.0)
        assertEquals(0.0, c.start, 1E-12)
        assertEquals(2.0, c.end, 1E-12)
        for (i in 0..20) {
            val t = i * 0.1
            assertEquals(curve.value(curve.end - t * 2.0), c.value(t), 1E-9)
        }
        assertEquals(curve.reverse(), curve.changeSpeed(-1.0))
    }
}
