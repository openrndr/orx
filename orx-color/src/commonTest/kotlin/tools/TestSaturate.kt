package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSaturate {
    private val seeds = listOf(
        ColorRGBa(0.8, 0.4, 0.2, linearity = Linearity.SRGB),
        ColorRGBa(0.3, 0.5, 0.7, linearity = Linearity.SRGB),
        ColorRGBa(0.8, 0.4, 0.2, linearity = Linearity.LINEAR)
    )

    private fun distance(a: ColorRGBa, b: ColorRGBa) = maxOf(abs(a.r - b.r), abs(a.g - b.g), abs(a.b - b.b))

    private inline fun <reified T> assertSaturates()
            where T : SaturatableColor<T>, T : ColorModel<T>, T : ConvertibleToColorRGBa {
        val name = T::class.simpleName
        for (seed in seeds) {
            // a factor of 1 keeps the color and its linearity
            val same = seed.saturate<T>(1.0)
            assertEquals(seed.linearity, same.linearity, "$name $seed")
            assertTrue(distance(seed, same) < 1E-4, "$name $seed $same")

            // a factor of 0 produces a neutral gray
            val gray = seed.saturate<T>(0.0).toSRGB()
            assertTrue(maxOf(gray.r, gray.g, gray.b) - minOf(gray.r, gray.g, gray.b) < 1E-4, "$name $seed $gray")

            // the saturation is scaled by the factor
            val saturation = seed.convertTo<T>().saturation
            val half = seed.saturate<T>(0.5).convertTo<T>().saturation
            assertEquals(saturation * 0.5, half, 1E-4, "$name $seed")
        }
    }

    @Test
    fun saturateWorksForAllSaturatableColors() {
        assertSaturates<ColorHSLa>()
        assertSaturates<ColorHSVa>()
        assertSaturates<ColorXSLa>()
        assertSaturates<ColorXSVa>()
        assertSaturates<ColorHSLUVa>()
        assertSaturates<ColorHPLUVa>()
        assertSaturates<ColorXSLUVa>()
        assertSaturates<ColorOKHSLa>()
        assertSaturates<ColorOKHSVa>()
        assertSaturates<ColorLSHUVa>()
        assertSaturates<ColorLSHABa>()
    }

    @Test
    fun convertToSupportsYxy() {
        val color = ColorRGBa(0.8, 0.4, 0.2, linearity = Linearity.SRGB)
        assertEquals(ColorYxya.fromXYZa(color.toXYZa()), color.convertTo<ColorYxya>())
    }
}
