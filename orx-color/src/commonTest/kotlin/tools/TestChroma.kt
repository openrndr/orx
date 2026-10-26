package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestChroma {
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

    private fun signedAngle(a: Double) = (((a % 360.0) + 360.0) % 360.0).let { if (it > 180.0) it - 360.0 else it }

    // lambdas instead of an inline reified function keep the test methods small
    private class Space(
        val name: String,
        val withChroma: (ColorRGBa, Double) -> ColorRGBa,
        val chroma: (ColorRGBa) -> Double,
        val hue: (ColorRGBa) -> Double,
        val luminosity: (ColorRGBa) -> Double,
        val modulateChroma: (ColorRGBa, Double) -> ColorRGBa,
        val mixChroma: (ColorRGBa, Double, Double) -> ColorRGBa,
        val clipChroma: (ColorRGBa) -> ColorRGBa
    )

    private fun assertChroma(space: Space) = with(space) {
        for (seed in seeds) {
            val c = chroma(seed)
            // setting the color's own chroma keeps the color and its linearity
            val same = withChroma(seed, c)
            assertEquals(seed.linearity, same.linearity, "$name $seed")
            assertTrue(distance(seed, same) < 1E-4, "$name $seed $same")

            // the chroma is set without changing hue and luminosity, modulating and mixing agree with setting it
            val half = withChroma(seed, c * 0.5)
            assertEquals(c * 0.5, chroma(half), c * 1E-4, "$name $seed")
            assertTrue(abs(signedAngle(hue(half) - hue(seed))) < 0.05, "$name $seed hue")
            assertEquals(luminosity(seed), luminosity(half), luminosity(seed) * 1E-4, "$name $seed luminosity")
            assertTrue(distance(half, modulateChroma(seed, 0.5)) < 1E-9, "$name $seed modulate")
            assertTrue(distance(half, mixChroma(seed, 0.0, 0.5)) < 1E-9, "$name $seed mix")

            // zero chroma is a neutral gray
            assertTrue(spread(withChroma(seed, 0.0)) < 1E-4, "$name $seed zero chroma")

            // clipping an out of gamut color finds the largest chroma in gamut and keeps hue and luminosity
            val outOfGamut = withChroma(seed, c * 3.0)
            assertTrue(outOfGamut.isOutOfGamut, "$name $seed $outOfGamut")
            val clipped = clipChroma(outOfGamut)
            assertTrue(!clipped.isOutOfGamut, "$name $seed $clipped")
            assertTrue(abs(signedAngle(hue(clipped) - hue(seed))) < 0.05, "$name $seed clipped hue")
            assertEquals(luminosity(seed), luminosity(clipped), luminosity(seed) * 2E-4, "$name $seed clipped luminosity")
            assertTrue(withChroma(seed, chroma(clipped) * 1.01).isOutOfGamut, "$name $seed clipped chroma is not maximal")
        }

        // neutral colors stay neutral
        for (neutral in neutrals) {
            assertTrue(distance(neutral, withChroma(neutral, 0.0)) < 1E-4, "$name $neutral")
            assertTrue(distance(neutral, modulateChroma(neutral, 2.0)) < 1E-4, "$name $neutral modulate")
            assertTrue(distance(neutral, clipChroma(neutral)) < 1E-4, "$name $neutral clip")
        }
    }

    @Test
    fun lchab() = assertChroma(Space("LCHABa",
        { c, v -> c.withChroma<ColorLCHABa>(v) }, { it.chroma<ColorLCHABa>() }, { it.convertTo<ColorLCHABa>().hue },
        { it.luminosity<ColorLCHABa>() }, { c, f -> c.modulateChroma<ColorLCHABa>(f) },
        { c, v, f -> c.mixChroma<ColorLCHABa>(v, f) }, { it.clipChroma<ColorLCHABa>() }))

    @Test
    fun lchuv() = assertChroma(Space("LCHUVa",
        { c, v -> c.withChroma<ColorLCHUVa>(v) }, { it.chroma<ColorLCHUVa>() }, { it.convertTo<ColorLCHUVa>().hue },
        { it.luminosity<ColorLCHUVa>() }, { c, f -> c.modulateChroma<ColorLCHUVa>(f) },
        { c, v, f -> c.mixChroma<ColorLCHUVa>(v, f) }, { it.clipChroma<ColorLCHUVa>() }))

    @Test
    fun oklch() = assertChroma(Space("OKLCHa",
        { c, v -> c.withChroma<ColorOKLCHa>(v) }, { it.chroma<ColorOKLCHa>() }, { it.convertTo<ColorOKLCHa>().hue },
        { it.luminosity<ColorOKLCHa>() }, { c, f -> c.modulateChroma<ColorOKLCHa>(f) },
        { c, v, f -> c.mixChroma<ColorOKLCHa>(v, f) }, { it.clipChroma<ColorOKLCHa>() }))
}
