package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestLuminosity {
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

    private fun spread(c: ColorRGBa) = c.toSRGB().let { maxOf(it.r, it.g, it.b) - minOf(it.r, it.g, it.b) }

    // lambdas instead of an inline reified function keep the test methods small
    private fun assertLuminosity(
        name: String,
        withLuminosity: (ColorRGBa, Double) -> ColorRGBa,
        luminosity: (ColorRGBa) -> Double,
        shadeLuminosity: (ColorRGBa, Double) -> ColorRGBa,
        mixLuminosity: (ColorRGBa, Double, Double) -> ColorRGBa
    ) {
        for (seed in seeds) {
            val l = luminosity(seed)
            // setting the color's own luminosity keeps the color and its linearity
            val same = withLuminosity(seed, l)
            assertEquals(seed.linearity, same.linearity, "$name $seed")
            assertTrue(distance(seed, same) < 1E-4, "$name $seed $same")

            // the luminosity is set, shading and mixing agree with setting it
            val darker = withLuminosity(seed, l * 0.6)
            assertEquals(l * 0.6, luminosity(darker), l * 1E-4, "$name $seed")
            assertTrue(distance(darker, shadeLuminosity(seed, 0.6)) < 1E-9, "$name $seed shade")
            assertTrue(distance(darker, mixLuminosity(seed, 0.0, 0.4)) < 1E-9, "$name $seed mix")
        }

        // neutral colors stay neutral
        val white = luminosity(ColorRGBa.WHITE)
        for (neutral in neutrals) {
            assertTrue(distance(neutral, withLuminosity(neutral, luminosity(neutral))) < 1E-4, "$name $neutral")
            val half = withLuminosity(neutral, white * 0.5)
            assertTrue(spread(half) < 1E-4, "$name $neutral $half")
            assertEquals(0.5, luminosity(half) / white, 1E-4, "$name $neutral")
        }
    }

    @Test
    fun labSpaces() {
        assertLuminosity("LABa", { c, l -> c.withLuminosity<ColorLABa>(l) }, { it.luminosity<ColorLABa>() },
            { c, f -> c.shadeLuminosity<ColorLABa>(f) }, { c, l, f -> c.mixLuminosity<ColorLABa>(l, f) })
        assertLuminosity("LCHABa", { c, l -> c.withLuminosity<ColorLCHABa>(l) }, { it.luminosity<ColorLCHABa>() },
            { c, f -> c.shadeLuminosity<ColorLCHABa>(f) }, { c, l, f -> c.mixLuminosity<ColorLCHABa>(l, f) })
        assertLuminosity("LSHABa", { c, l -> c.withLuminosity<ColorLSHABa>(l) }, { it.luminosity<ColorLSHABa>() },
            { c, f -> c.shadeLuminosity<ColorLSHABa>(f) }, { c, l, f -> c.mixLuminosity<ColorLSHABa>(l, f) })
    }

    @Test
    fun luvSpaces() {
        assertLuminosity("LUVa", { c, l -> c.withLuminosity<ColorLUVa>(l) }, { it.luminosity<ColorLUVa>() },
            { c, f -> c.shadeLuminosity<ColorLUVa>(f) }, { c, l, f -> c.mixLuminosity<ColorLUVa>(l, f) })
        assertLuminosity("LCHUVa", { c, l -> c.withLuminosity<ColorLCHUVa>(l) }, { it.luminosity<ColorLCHUVa>() },
            { c, f -> c.shadeLuminosity<ColorLCHUVa>(f) }, { c, l, f -> c.mixLuminosity<ColorLCHUVa>(l, f) })
        assertLuminosity("LSHUVa", { c, l -> c.withLuminosity<ColorLSHUVa>(l) }, { it.luminosity<ColorLSHUVa>() },
            { c, f -> c.shadeLuminosity<ColorLSHUVa>(f) }, { c, l, f -> c.mixLuminosity<ColorLSHUVa>(l, f) })
        assertLuminosity("HSLUVa", { c, l -> c.withLuminosity<ColorHSLUVa>(l) }, { it.luminosity<ColorHSLUVa>() },
            { c, f -> c.shadeLuminosity<ColorHSLUVa>(f) }, { c, l, f -> c.mixLuminosity<ColorHSLUVa>(l, f) })
        assertLuminosity("HPLUVa", { c, l -> c.withLuminosity<ColorHPLUVa>(l) }, { it.luminosity<ColorHPLUVa>() },
            { c, f -> c.shadeLuminosity<ColorHPLUVa>(f) }, { c, l, f -> c.mixLuminosity<ColorHPLUVa>(l, f) })
    }

    @Test
    fun okSpaces() {
        assertLuminosity("OKLABa", { c, l -> c.withLuminosity<ColorOKLABa>(l) }, { it.luminosity<ColorOKLABa>() },
            { c, f -> c.shadeLuminosity<ColorOKLABa>(f) }, { c, l, f -> c.mixLuminosity<ColorOKLABa>(l, f) })
        assertLuminosity("OKLCHa", { c, l -> c.withLuminosity<ColorOKLCHa>(l) }, { it.luminosity<ColorOKLCHa>() },
            { c, f -> c.shadeLuminosity<ColorOKLCHa>(f) }, { c, l, f -> c.mixLuminosity<ColorOKLCHa>(l, f) })
    }
}
