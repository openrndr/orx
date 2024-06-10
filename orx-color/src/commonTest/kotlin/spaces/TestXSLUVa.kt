package org.openrndr.extra.color.spaces

import org.openrndr.color.ColorRGBa
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class TestXSLUVa {

    @Test
    fun testHueConversions() {
        for (i in 0 until 3600) {
            val inputHue = i/10.0
            val x = hueToX(inputHue)
            val recoveredHue = xToHue(x)
            assertTrue( abs(recoveredHue-inputHue) < 1E-8, "$inputHue $recoveredHue")
        }
    }

    @Test
    fun testConversions() {
        val testColors = listOf(ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.GREEN, ColorRGBa.GRAY, ColorRGBa.YELLOW)
        val error = (-1E-3..1E-3)
        testColors.forEach {
            val testColor = it
            val hsluvColor = it.toHSLUVa()
            val xsluvColor = it.toXSLUVa()

            val restoredHsluvColor = xsluvColor.toHSLUVa()

            val dh = restoredHsluvColor.h - hsluvColor.h
            val dl = restoredHsluvColor.l - hsluvColor.l
            val ds = restoredHsluvColor.s - hsluvColor.s

            assertTrue(abs(dh) < 1E-7)

        }
    }

}