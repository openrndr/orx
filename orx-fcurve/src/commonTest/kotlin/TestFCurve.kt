import org.openrndr.extra.fcurve.fcurve
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFCurve {
    @Test
    fun testConstantExpression() {
        val text = "10.5"
        val fc = fcurve(text)
        assertEquals(10.5, fc.value(0.0))
        assertEquals(10.5, fc.value(1.0))
        assertEquals(10.5, fc.value(-1.0))

        val normalizedSampler = fc.sampler(true)
        assertEquals(10.5, normalizedSampler(0.0))
        assertEquals(10.5, normalizedSampler(1.0))
        assertEquals(10.5, normalizedSampler(-1.0))
    }

    @Test
    fun testAbsoluteHold() {
        run {
            val text = "H-1 L 5 5"
            val fc = fcurve(text)
            assertEquals(0.0, fc.value(-1.0))
            assertEquals(5.0, fc.value(4.0))
            assertEquals(-1.0, fc.start)
            assertEquals(4.0, fc.end)
            assertEquals(5.0, fc.duration)
        }
        run {
            val text = "H1 L 5 5"
            val fc = fcurve(text)
            assertEquals(0.0, fc.value(1.0))
            assertEquals(5.0, fc.value(6.0))
            assertEquals(1.0, fc.start)
            assertEquals(6.0, fc.end)
            assertEquals(5.0, fc.duration)
        }
    }
}