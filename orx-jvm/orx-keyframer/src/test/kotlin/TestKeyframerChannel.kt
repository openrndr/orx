import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.keyframer.KeyframerChannel
import org.openrndr.extra.easing.Easing

import kotlin.test.Test

class TestKeyframerChannel {
    @Test
    fun `a keyframer channel without keys`() {
        val kfc = KeyframerChannel()
        kfc.value(0.0) `should be` null
    }

    @Test
    fun `a keyframer channel with a single key`() {
        val kfc = KeyframerChannel()

        kfc.add(0.0, 1.0, Easing.Linear.function)
        kfc.value(0.0)?.shouldBeNear(1.0, 10E-6)
        kfc.value(-1.0) `should be` null
    }

    @Test
    fun `a keyframer channel with two keys`() {
        val kfc = KeyframerChannel()
        kfc.add(0.0, 1.0, Easing.Linear.function)
        kfc.add(1.0, 2.0, Easing.Linear.function)
        kfc.value(0.0)?.shouldBeNear(1.0, 10E-6)
        kfc.value(-1.0) `should be` null
    }
}