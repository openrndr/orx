package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestShiftHue {
    private val seeds = listOf(
        ColorRGBa(0.8, 0.4, 0.2, linearity = Linearity.SRGB),
        ColorRGBa(0.3, 0.5, 0.7, linearity = Linearity.SRGB),
        ColorRGBa(0.8, 0.4, 0.2, linearity = Linearity.LINEAR)
    )

    private val neutrals = listOf(
        ColorRGBa.BLACK, ColorRGBa.WHITE,
        ColorRGBa(0.5, 0.5, 0.5, linearity = Linearity.SRGB), ColorRGBa(0.5, 0.5, 0.5, linearity = Linearity.LINEAR)
    )

    private fun distance(a: ColorRGBa, b: ColorRGBa) = maxOf(abs(a.r - b.r), abs(a.g - b.g), abs(a.b - b.b))

    private fun signedAngle(a: Double) = (((a % 360.0) + 360.0) % 360.0).let { if (it > 180.0) it - 360.0 else it }

    // lambdas instead of an inline reified function keep the test methods small
    private fun assertShiftsHue(
        name: String,
        shift: (ColorRGBa, Double) -> ColorRGBa,
        hue: (ColorRGBa) -> Double
    ) {
        for (seed in seeds) {
            // whole turns keep the color and its linearity
            for (degrees in listOf(0.0, 360.0, -360.0, 720.0)) {
                val same = shift(seed, degrees)
                assertEquals(seed.linearity, same.linearity, "$name $seed $degrees")
                assertTrue(distance(seed, same) < 1E-4, "$name $seed $degrees $same")
            }
            assertTrue(distance(seed, shift(shift(seed, 180.0), 180.0)) < 1E-4, "$name $seed 180 + 180")

            // the hue is shifted by the given amount
            for (degrees in listOf(30.0, -30.0, 200.0)) {
                val error = signedAngle(hue(shift(seed, degrees)) - hue(seed) - degrees)
                assertTrue(abs(error) < 0.05, "$name $seed $degrees: hue error $error")
            }
        }
        // colors without a hue are not changed
        for (neutral in neutrals) {
            val shifted = shift(neutral, 90.0)
            assertTrue(distance(neutral, shifted) < 1E-4, "$name $neutral $shifted")
        }
    }

    @Test
    fun hueSaturationSpaces() {
        assertShiftsHue("HSLa", { c, d -> c.shiftHue<ColorHSLa>(d) }, { it.convertTo<ColorHSLa>().hue })
        assertShiftsHue("HSVa", { c, d -> c.shiftHue<ColorHSVa>(d) }, { it.convertTo<ColorHSVa>().hue })
        assertShiftsHue("XSLa", { c, d -> c.shiftHue<ColorXSLa>(d) }, { it.convertTo<ColorXSLa>().hue })
        assertShiftsHue("XSVa", { c, d -> c.shiftHue<ColorXSVa>(d) }, { it.convertTo<ColorXSVa>().hue })
        assertShiftsHue("OKHSLa", { c, d -> c.shiftHue<ColorOKHSLa>(d) }, { it.convertTo<ColorOKHSLa>().hue })
        assertShiftsHue("OKHSVa", { c, d -> c.shiftHue<ColorOKHSVa>(d) }, { it.convertTo<ColorOKHSVa>().hue })
    }

    @Test
    fun luvSpaces() {
        assertShiftsHue("LCHUVa", { c, d -> c.shiftHue<ColorLCHUVa>(d) }, { it.convertTo<ColorLCHUVa>().hue })
        assertShiftsHue("HSLUVa", { c, d -> c.shiftHue<ColorHSLUVa>(d) }, { it.convertTo<ColorHSLUVa>().hue })
        assertShiftsHue("HPLUVa", { c, d -> c.shiftHue<ColorHPLUVa>(d) }, { it.convertTo<ColorHPLUVa>().hue })
        assertShiftsHue("XSLUVa", { c, d -> c.shiftHue<ColorXSLUVa>(d) }, { it.convertTo<ColorXSLUVa>().hue })
        assertShiftsHue("LSHUVa", { c, d -> c.shiftHue<ColorLSHUVa>(d) }, { it.convertTo<ColorLSHUVa>().hue })
    }

    @Test
    fun labSpaces() {
        assertShiftsHue("LCHABa", { c, d -> c.shiftHue<ColorLCHABa>(d) }, { it.convertTo<ColorLCHABa>().hue })
        assertShiftsHue("OKLCHa", { c, d -> c.shiftHue<ColorOKLCHa>(d) }, { it.convertTo<ColorOKLCHa>().hue })
        assertShiftsHue("LSHABa", { c, d -> c.shiftHue<ColorLSHABa>(d) }, { it.convertTo<ColorLSHABa>().hue })
    }
}
