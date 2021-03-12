import org.amshove.kluent.`should be equal to`
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extras.color.palettes.rangeTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestMix : Spek({

    describe("two srgb colors") {
        val a = ColorRGBa.BLUE
        val b = ColorRGBa.RED

        a.linearity `should be equal to` Linearity.SRGB

        it("should mix properly") {
            a.mix(b, 0.0) `should be equal to` a
            a.mix(b, 1.0) `should be equal to` b
        }
    }

    describe("two linear rgb colors") {
        val a = ColorRGBa.BLUE.toLinear()
        val b = ColorRGBa.RED.toLinear()

        it("should mix properly") {
            a.mix(b, 0.0) `should be equal to` a
            a.mix(b, 1.0) `should be equal to` b
        }
    }

    describe("a 2-step range of colors") {
        val a = ColorRGBa.BLUE
        val b = ColorRGBa.RED

        val blend = a..b blend 2
        blend.size `should be equal to` 2
        blend[0] `should be equal to` a
        blend[1] `should be equal to` b
    }

})