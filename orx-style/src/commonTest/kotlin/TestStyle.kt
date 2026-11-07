import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extra.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestStyle {
    private fun assertColor(expected: ColorRGBa, actual: ColorRGBa?, tolerance: Double = 1E-3) {
        assertNotNull(actual)
        val e = expected.toSRGB()
        val a = actual.toSRGB()
        assertEquals(e.r, a.r, tolerance)
        assertEquals(e.g, a.g, tolerance)
        assertEquals(e.b, a.b, tolerance)
        assertEquals(e.alpha, a.alpha, tolerance)
    }

    @Test
    fun parsesCssColors() {
        assertColor(ColorRGBa.fromHex(0xff0000), parseCssColor("red"))
        assertColor(ColorRGBa.fromHex(0xff0000), parseCssColor(" RED "))
        assertColor(ColorRGBa.fromHex(0x112233), parseCssColor("#123"))
        assertEquals(0.0, parseCssColor("transparent")?.alpha)
        assertColor(ColorRGBa.fromHex(0x336699).opacify(0.5), parseCssColor("rgb(51 102 153 / 50%)"))
        assertColor(ColorRGBa.fromHex(0x00ff00), parseCssColor("hsl(120, 100%, 50%)"))
        assertColor(ColorRGBa.fromHex(0x00ff00), parseCssColor("hsl(0.3333turn 100% 50%)"), 2E-3)
        // oklch of pure red, and a mix of red and blue in srgb
        assertColor(ColorRGBa.fromHex(0xff0000), parseCssColor("oklch(62.8% 0.2577 29.23)"), 5E-3)
        assertColor(ColorRGBa(0.5, 0.0, 0.5, linearity = Linearity.SRGB), parseCssColor("color-mix(in srgb, red, blue)"))
        assertColor(ColorRGBa(0.25, 0.0, 0.75, linearity = Linearity.SRGB), parseCssColor("color-mix(in srgb, red 25%, #00f)"))
        assertNull(parseCssColor("currentcolor"))
        assertNull(parseCssColor("#12"))
        assertNull(parseCssColor("rgb(1, 2)"))
        assertTrue(isCssColorName("rebeccapurple"))
        assertEquals(listOf("a(1, 2)", " b"), splitCssList("a(1, 2), b"))
        assertEquals(90.0, parseCssAngle("0.25turn")!!, 1E-9)
    }

    @Test
    fun timingFunctions() {
        assertEquals(0.5, TimingFunction.LINEAR.ease(0.5))
        // ease-in-out is symmetric
        assertEquals(0.5, TimingFunction.css("ease-in-out").ease(0.5), 1E-6)
        assertTrue(TimingFunction.css("ease-in").ease(0.25) < 0.25)
        assertEquals(0.5, TimingFunction.css("steps(2)").ease(0.6))
        assertEquals(1.0, TimingFunction.css("step-start").ease(0.1))
        assertEquals(0.5, TimingFunction.css("cubic-bezier(0, 0, 1, 1)").ease(0.5), 1E-6)
        assertEquals(0.25, parseCssTime("250ms"))
        assertEquals(90.0, parseCssTime("01:30"))
    }

    @Test
    fun transitionTimingOfDeclarations() {
        val declarations = mapOf("transition" to "opacity 0.5s ease-in 100ms, color 1s")
        val opacity = TransitionTiming.css("opacity") { declarations[it] }
        assertEquals(0.5, opacity?.duration)
        assertEquals(0.1, opacity?.delay)
        assertNull(TransitionTiming.css("width") { declarations[it] })
        // longhands override the shorthand, their lists repeat
        val longhands = mapOf(
            "transition" to "all 3s",
            "transition-property" to "color, background-color, opacity",
            "transition-duration" to "1s, 2s",
            "transition-timing-function" to "linear"
        )
        assertEquals(1.0, TransitionTiming.css("opacity") { longhands[it] }?.duration)
        assertEquals(2.0, TransitionTiming.css("background-color") { longhands[it] }?.duration)
        assertNull(TransitionTiming.css("width") { longhands[it] })
        // all transitions every property
        assertEquals(3.0, TransitionTiming.css("width") { mapOf("transition" to "all 3s")[it] }?.duration)
    }

    @Test
    fun transitionsStartFromTheShownValue() {
        val transitions = Transitions<Double> { _, a, b, t -> a + (b - a) * t }
        val events = mutableListOf<String>()
        transitions.started = { events.add("start ${it.property}") }
        transitions.ended = { events.add("end ${it.property}") }
        transitions.canceled = { events.add("cancel ${it.property}") }
        val timing = TransitionTiming(1.0, 0.5, TimingFunction.LINEAR)
        transitions.change("x", 0.0, 10.0, 0.0, timing)
        // during the delay the value is the start value
        assertEquals(0.0, transitions.value("x", 0.25))
        assertEquals(5.0, transitions.value("x", 1.0)!!, 1E-9)
        // reversing starts from the value that is shown
        transitions.change("x", 10.0, 0.0, 1.0, TransitionTiming(1.0, 0.0, TimingFunction.LINEAR))
        assertEquals(2.5, transitions.value("x", 1.5)!!, 1E-9)
        assertNull(transitions.value("x", 2.5))
        assertEquals(listOf("start x", "cancel x", "start x", "end x"), events)
        // without timing nothing transitions
        transitions.change("x", 0.0, 1.0, 3.0, null)
        assertNull(transitions.value("x", 3.1))
    }
}
