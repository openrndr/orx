package org.openrndr.extra.color.mixing

import org.openrndr.color.ColorRGBa
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares the port with the examples in the spectral.js 3.0.0 README.
 */
class TestSpectralJs {
    private fun hex(color: ColorRGBa): String {
        val s = color.toSRGB()
        return "#" + listOf(s.r, s.g, s.b).joinToString("") {
            (it.coerceIn(0.0, 1.0) * 255.0).roundToInt().toString(16).padStart(2, '0').uppercase()
        }
    }

    // the port converts XYZ to RGB with the openrndr matrices, allow a difference of 1 in 8 bit
    private fun assertHex(expected: String, actual: ColorRGBa) {
        val e = ColorRGBa.fromHex(expected).toSRGB()
        val a = actual.toSRGB()
        val difference = maxOf(abs(e.r - a.r), abs(e.g - a.g), abs(e.b - a.b)) * 255.0
        assertTrue(difference <= 1.5, "expected $expected, got ${hex(actual)}")
    }

    private fun spectral(hex: String, tintingStrength: Double = 1.0) = SpectralColor(ColorRGBa.fromHex(hex), tintingStrength)

    @Test
    fun mix() {
        assertHex("#3D933E", mixSpectral(spectral("#002185") to 0.5, spectral("#FCD200") to 0.5).toRGBa(SpectralGamutMapping.MAP))
        assertHex(
            "#8D7964",
            mixSpectral(spectral("#FCF046") to 1.0, spectral("#E53166") to 1.0, spectral("#3375DA") to 1.0)
                .toRGBa(SpectralGamutMapping.MAP)
        )
        // the RGB version and the two color version give the same result
        assertHex("#3D933E", mixSpectral(ColorRGBa.fromHex("#002185") to 0.5, ColorRGBa.fromHex("#FCD200") to 0.5))
        assertHex("#3D933E", mixSpectral(ColorRGBa.fromHex("#002185"), ColorRGBa.fromHex("#FCD200"), 0.5))
    }

    @Test
    fun palette() {
        val expected = listOf("#BB0657", "#C00C5C", "#CD1E6F", "#DD3889", "#E85AA0", "#ED85AF", "#E9B9B4", "#E0E0B3")
        val palette = spectralPalette(ColorRGBa.fromHex("#BB0657"), ColorRGBa.fromHex("#E0E0B3"), 8)
        assertEquals(8, palette.size)
        for ((e, a) in expected.zip(palette)) assertHex(e, a)
    }

    @Test
    fun gradient() {
        val gradient = spectralGradient(
            0.75, ColorRGBa.fromHex("#005E72") to 0.0, ColorRGBa.fromHex("#EAD9A7") to 0.5, ColorRGBa.fromHex("#894B54") to 1.0
        )
        assertHex("#C7938C", gradient)
        // outside the stops the gradient has the color of the nearest stop
        assertHex("#005E72", spectralGradient(-1.0, ColorRGBa.fromHex("#005E72") to 0.0, ColorRGBa.fromHex("#894B54") to 1.0))
        assertHex("#894B54", spectralGradient(2.0, ColorRGBa.fromHex("#005E72") to 0.0, ColorRGBa.fromHex("#894B54") to 1.0))
    }

    @Test
    fun tintingStrength() {
        assertHex("#FF440F", mixSpectral(spectral("#ff0000") to 0.5, spectral("#ffff00") to 0.5).toRGBa(SpectralGamutMapping.MAP))
        assertHex(
            "#FF8427",
            mixSpectral(spectral("#ff0000", 0.35) to 0.5, spectral("#ffff00") to 0.5).toRGBa(SpectralGamutMapping.MAP)
        )
    }

    @Test
    fun gamutMapping() {
        // green and blue mix to a color with a negative red component
        val mix = mixSpectral(SpectralColor(ColorRGBa.GREEN) to 0.5, SpectralColor(ColorRGBa.BLUE) to 0.5)
        val raw = mix.toRGBa(SpectralGamutMapping.NONE)
        assertTrue(raw.r < 0.0, "$raw")
        for (mapping in listOf(SpectralGamutMapping.CLIP, SpectralGamutMapping.MAP)) {
            val mapped = mix.toRGBa(mapping)
            assertTrue(listOf(mapped.r, mapped.g, mapped.b).all { it in 0.0..1.0 }, "$mapping $mapped")
        }
    }
}
