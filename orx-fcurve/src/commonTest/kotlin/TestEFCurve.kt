import org.openrndr.extra.fcurve.efcurve
import org.openrndr.extra.fcurve.fcurve
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEFCurve {
    @Test
    fun comments() {
        val text = """M1 (h5 m3){
        |10.3 # toch wel handig zo'n comment
        |11.2
        |14.5
        |}
    """.trimMargin()
        assertEquals("M1 h5 m3 h5 m3 h5 m3", efcurve(text))
    }

    @Test
    fun expressions() {
        //assertEquals("M${9.toDouble()}", efcurve("M{4.0 + 5.0}"))
    }

    @Test
    fun listExpansion() {
        //assertEquals("M0 L1.0, ${3.toDouble()} L1.0, ${6.0}", efcurve("M0 (L1.0, {it}){3, 6}"))
    }

    @Test
    fun repetition() {
        //assertEquals("M0 L1.0, 3.0 L1.0, 3.0", efcurve("M0 (L1.0, 3.0)[2]"))
        //assertEquals("M0 L1.0, ${0.0} L1.0, ${1.0}", efcurve("M0 (L1.0, {it})[2]"))
        //assertEquals("M0 L1.0, ${0.0} L1.0, ${1.0} L1.0, ${0.0} L1.0, ${1.0} L1.0, ${0.0} L1.0, ${1.0}", efcurve("M0 ((L1.0, {it})[2])[3]"))
    }

    @Test
    fun testContinuity() {
        val fc = fcurve("Q1 25% 3 100 Q1 25% 3 0 Q1 25% 3 100 Q1 25% 3 0")
        val s = fc.sampler()
        assertEquals(0.0,  s(6.0))
    }

    @Test
    fun testConstantValue() {
        val text = "10.5"
        val fc = fcurve(efcurve(text))
        assertEquals(10.5, fc.value(0.0, null))
        assertEquals(10.5, fc.value(1.0, null))
        assertEquals(10.5, fc.value(-1.0, null))

        val normalizedSampler = fc.sampler(true)
        assertEquals(10.5, normalizedSampler(0.0))
        assertEquals(10.5, normalizedSampler(1.0))
        assertEquals(10.5, normalizedSampler(-1.0))
    }
    @Test
    fun testConstantExpression() {
        val text = "${21.0 / 2.0}"
        val fc = fcurve(efcurve(text))
        assertEquals(10.5, fc.value(0.0, null))
        assertEquals(10.5, fc.value(1.0, null))
        assertEquals(10.5, fc.value(-1.0, null))

        val normalizedSampler = fc.sampler(true)
        assertEquals(10.5, normalizedSampler(0.0))
        assertEquals(10.5, normalizedSampler(1.0))
        assertEquals(10.5, normalizedSampler(-1.0))
    }
}