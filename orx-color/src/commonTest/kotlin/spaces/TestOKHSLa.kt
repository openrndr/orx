package org.openrndr.extra.color.spaces

import org.openrndr.color.ColorRGBa
import kotlin.test.Test
import kotlin.test.assertTrue

class TestOKHSLa {
    @Test
    fun testConversions() {
        val testColors = listOf(ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.GREEN, ColorRGBa.GRAY, ColorRGBa.YELLOW)
        val error = (-1E-5 .. 1E-5)
        testColors.forEach {
            val testColor = it
            val toColor = it.toOKHSLa()
            val restoreColor = toColor.toRGBa()
            assertTrue("color $testColor, $toColor, $restoreColor") {
                testColor.r - restoreColor.r in error && testColor.g - restoreColor.g in error && testColor.b - restoreColor.b in error
            }
        }
    }

    @Test
    fun testSaturationPersistence() {
        val black = ColorRGBa.BLACK.toOKHSLa()

        assertTrue("resulting OKHSLa $black contains no NaNs") {
            black.h == black.h && black.s == black.s && black.l == black.l
        }

        val rgbBlack = black.toRGBa()
        val white = ColorRGBa.WHITE.toOKHSLa()
        val rgbWhite = white.toRGBa()
        val epsilon = 1E-6
        assertTrue("resulting color $rgbWhite is white") {
            rgbWhite.r in (1.0 - epsilon .. 1.0 + epsilon) && rgbWhite.g in (1.0 - epsilon .. 1.0 + epsilon) && rgbWhite.b in (1.0 - epsilon .. 1.0 + epsilon)
        }
        assertTrue("resulting color $rgbBlack is black") {
            rgbBlack.r in (0.0 - epsilon .. 0.0 + epsilon) && rgbBlack.g in (0.0 - epsilon .. 0.0 + epsilon) && rgbBlack.b in (0.0 - epsilon .. 0.0 + epsilon)
        }
    }
}