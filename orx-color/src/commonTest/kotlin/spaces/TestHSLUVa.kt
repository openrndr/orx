package org.openrndr.extra.color.spaces

import org.openrndr.color.ColorRGBa
import kotlin.test.Test
import kotlin.test.assertTrue

class TestHSLUVa {

    @Test
    fun testConversions() {
        val testColors = listOf(ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.GREEN, ColorRGBa.GRAY, ColorRGBa.YELLOW)
        val error = (-1E-3 .. 1E-3)
        testColors.forEach {
            val testColor = it
            val toColor = it.toHSLUVa()
            val restoreColor = toColor.toRGBa().toSRGB()
            assertTrue("color $testColor, $toColor, $restoreColor") {
                testColor.r - restoreColor.r in error && testColor.g - restoreColor.g in error && testColor.b - restoreColor.b in error
            }
        }
    }

}