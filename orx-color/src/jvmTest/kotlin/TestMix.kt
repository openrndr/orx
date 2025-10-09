//import org.openrndr.color.ColorRGBa
//import org.openrndr.color.Linearity
//import org.openrndr.extra.color.palettes.rangeTo
//
//import kotlin.test.assertEquals
//import kotlin.test.assertSame
//
//class TestMix {
//
//    describe("two srgb colors") {
//        val a = ColorRGBa.BLUE
//        val b = ColorRGBa.RED
//
//        assertEquals(Linearity.SRGB, a.linearity)
//
//
//        it("should mix properly") {
//            assertSame(a, a.mix(b, 0.0))
//            assertSame(b, a.mix(b, 1.0))
//        }
//    }
//
//    describe("two linear rgb colors") {
//        val a = ColorRGBa.BLUE.toLinear()
//        val b = ColorRGBa.RED.toLinear()
//
//        it("should mix properly") {
//            assertSame(a, a.mix(b, 0.0))
//            assertSame(b, a.mix(b, 1.0))
//        }
//    }
//
//    describe("a 2-step range of colors") {
//        val a = ColorRGBa.BLUE
//        val b = ColorRGBa.RED
//
//        val blend = a..b blend 2
//        assertEquals(2, blend.size)
//        assertEquals(a, blend[0])
//        assertEquals(b, blend[1])
//    }
//
//})