import org.openrndr.extra.easing.Easing
import org.openrndr.extra.keyframer.KeyframerChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestKeyframerChannel {
    @Test
    fun `a keyframer channel without keys`() {
        val kfc = KeyframerChannel()
        assertNull(kfc.value(0.0))
    }

    @Test
    fun `a keyframer channel with a single key`() {
        val kfc = KeyframerChannel()

        kfc.add(0.0, 1.0, Easing.Linear.function)
        val value = kfc.value(0.0)
        if (value != null) {
            assertEquals(1.0, value, 10E-6)
        }
        assertNull(kfc.value(-1.0))
    }

    @Test
    fun `a keyframer channel with two keys`() {
        val kfc = KeyframerChannel()
        kfc.add(0.0, 1.0, Easing.Linear.function)
        kfc.add(1.0, 2.0, Easing.Linear.function)
        val value = kfc.value(0.0)
        if (value != null) {
            assertEquals(1.0, value, 10E-6)
        }
        assertNull(kfc.value(-1.0))
    }
}