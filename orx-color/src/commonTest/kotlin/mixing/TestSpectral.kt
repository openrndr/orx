package org.openrndr.extra.color.mixing

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extra.color.tools.matchLinearity
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSpectral {
    // compared in linear RGB, mixSpectral returns the linearity of its first color
    private fun distance(a: ColorRGBa, b: ColorRGBa): Double {
        val la = a.toLinear()
        val lb = b.toLinear()
        return maxOf(abs(la.r - lb.r), abs(la.g - lb.g), abs(la.b - lb.b))
    }

    // colors whose reflectance stays above the front surface reflection of glossy paint (k1 = 0.04)
    private val colors = listOf(
        ColorRGBa(0.6, 0.3, 0.1, linearity = Linearity.LINEAR),
        ColorRGBa(0.2, 0.4, 0.7, linearity = Linearity.LINEAR),
        ColorRGBa(0.8, 0.7, 0.3, linearity = Linearity.SRGB)
    )

    @Test
    fun testIdentity() {
        for (c in listOf(ColorRGBa.RED, ColorRGBa.GREEN, ColorRGBa.BLUE)) {
            val r = linearToReflectance(c)
            assertEquals(38, r.size)
            val xyz = reflectanceToXYZ(r)
            val cp = xyz.toRGBa().matchLinearity(c)
            assertTrue(cp.toVector4().distanceTo(c.toVector4()) < 5E-3)
        }
    }

    @Test
    fun testSaunderson() {
        // without surface reflections the correction does nothing
        for (r in listOf(0.0, 0.1, 0.5, 1.0)) {
            assertEquals(r, saundersonCorrection(r, 0.0, 0.0))
            assertEquals(r, inverseSaundersonCorrection(r, 0.0, 0.0).let { if (r == 0.0) 0.0 else it })
        }

        val k1 = 0.04
        val k2 = 0.6
        // a black layer still reflects at its surface, a perfect white layer reflects everything
        assertEquals(k1, saundersonCorrection(0.0, k1, k2), 1E-12)
        assertEquals(1.0, saundersonCorrection(1.0, k1, k2), 1E-12)

        // the inverse undoes the correction
        for (i in 1..20) {
            val r = i / 20.0
            assertEquals(r, inverseSaundersonCorrection(saundersonCorrection(r, k1, k2), k1, k2), 1E-12)
        }
    }

    @Test
    fun testMixSpectralWithSurfaceReflection() {
        val k1 = 0.04
        val k2 = 0.6
        for (c in colors) {
            // mixing a color with itself returns the color
            for (t in listOf(0.0, 0.3, 1.0)) {
                val mixed = mixSpectral(c, c, t, k1, k2)
                assertTrue(distance(c, mixed) < 2E-3, "$c $t $mixed")
            }
            // the ends of the mix are the input colors
            for (other in colors) {
                assertTrue(distance(c, mixSpectral(c, other, 0.0, k1, k2)) < 2E-3, "$c $other")
                assertTrue(distance(other, mixSpectral(c, other, 1.0, k1, k2)) < 2E-3, "$c $other")
            }
        }
    }

    @Test
    fun testMixSpectralWithoutSurfaceReflection() {
        for (c in colors) {
            assertTrue(distance(c, mixSpectral(c, c, 0.5)) < 2E-3, "$c")
        }
        // blue and yellow paint make green
        val green = mixSpectral(ColorRGBa.BLUE, ColorRGBa.YELLOW, 0.5)
        assertTrue(green.g > green.r && green.g > green.b, "$green")
    }

    @Test
    fun testColorCheckerReflectances() {
        val all = ColorCheckerReflectances.all
        assertEquals(24, all.size)
        for ((name, reflectance) in all) {
            assertEquals(38, reflectance.size, name)
            assertTrue(reflectance.all { it in 0.0..1.0 }, name)
        }

        // the neutral patches are gray and get darker from white to black
        val neutrals = with(ColorCheckerReflectances) { listOf(white, neutral8, neutral65, neutral5, neutral35, black) }
            .map { reflectanceToRGBa(it).toSRGB() }
        for (n in neutrals) {
            assertTrue(maxOf(n.r, n.g, n.b) - minOf(n.r, n.g, n.b) < 0.02, "$n")
        }
        assertTrue(neutrals.zipWithNext().all { (a, b) -> a.g > b.g })
        // white 9.5 is (243, 243, 242) in sRGB
        assertEquals(243.0 / 255.0, neutrals.first().g, 2.0 / 255.0)

        val red = reflectanceToRGBa(ColorCheckerReflectances.red)
        val green = reflectanceToRGBa(ColorCheckerReflectances.green)
        val blue = reflectanceToRGBa(ColorCheckerReflectances.blue)
        assertTrue(red.r > red.g && red.r > red.b, "$red")
        assertTrue(green.g > green.r && green.g > green.b, "$green")
        assertTrue(blue.b > blue.r && blue.b > blue.g, "$blue")

        // properties return copies
        ColorCheckerReflectances.white[0] = 0.0
        assertTrue(ColorCheckerReflectances.white[0] > 0.0)
    }

    @Test
    fun testMixReflectance() {
        val yellow = ColorCheckerReflectances.yellow
        val blue = ColorCheckerReflectances.blue

        // mixing a reflectance with itself returns it
        for ((k1, k2) in listOf(0.0 to 0.0, 0.04 to 0.6)) {
            val mixed = mixReflectance(yellow, yellow, 0.3, k1, k2)
            for (i in yellow.indices) {
                assertEquals(yellow[i], mixed[i], 1E-9 + if (yellow[i] < k1) k1 else 0.0)
            }
        }

        // blue and yellow paint make green
        val green = reflectanceToRGBa(mixReflectance(blue, yellow, 0.5))
        assertTrue(green.g > green.r && green.g > green.b, "$green")
    }
}
