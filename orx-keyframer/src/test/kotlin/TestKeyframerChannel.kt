import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.keyframer.KeyframerChannel
import org.openrndr.extras.easing.Easing
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestKeyframerChannel : Spek({

    describe("a keyframer channel without keys") {
        val kfc = KeyframerChannel()
        it ("should return null when asking for value before first key time") {
            kfc.value(0.0) `should be` null
        }
    }
    describe("a keyframer channel with a single key") {
        val kfc = KeyframerChannel()
        kfc.add(0.0, 1.0, Easing.Linear.function)
        kfc.value(0.0)?.shouldBeNear(1.0, 10E-6)

        it ("should return null when asking for value before first key time") {
            kfc.value(-1.0) `should be` null
        }
    }
    describe("a keyframer channel with two keys") {
        val kfc = KeyframerChannel()
        kfc.add(0.0, 1.0, Easing.Linear.function)
        kfc.add(1.0, 2.0, Easing.Linear.function)
        kfc.value(0.0)?.shouldBeNear(1.0, 10E-6)

        it ("should return null when asking for value before first key time") {
            kfc.value(-1.0) `should be` null
        }
    }
})