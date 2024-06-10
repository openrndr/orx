package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestColorRGBaExtensions {
    @Test
    fun testShiftHue0() {
        val seed = ColorRGBa.RED.shade(0.5)
        val shifted = listOf(
            seed.shiftHue<LCHab>(0.0),
            seed.shiftHue<LCHuv>(0.0),
            seed.shiftHue<OKLCH>(0.0),
            seed.shiftHue<HSLuv>(0.0)
        )
        for (s in shifted) {
            assertEquals(Linearity.SRGB, s.linearity)
            assertTrue(seed.toVector4().distanceTo(s.toVector4()) < 1E-4)
        }
    }

    @Test
    fun testShiftHue0Linear() {
        val seed = ColorRGBa.RED.shade(0.5).toLinear()
        val shifted = listOf(
            seed.shiftHue<LCHab>(0.0),
            seed.shiftHue<LCHuv>(0.0),
            seed.shiftHue<OKLCH>(0.0),
            seed.shiftHue<HSLuv>(0.0)
        )
        for (s in shifted) {
            assertEquals(Linearity.LINEAR, s.linearity)
            assertTrue(seed.toVector4().distanceTo(s.toVector4()) < 1E-4)
        }
    }

    @Test
    fun testSaturate1() {
        val seed = ColorRGBa.RED.shade(0.5)
        val shifted = listOf(
            seed.saturate<HSV>(1.0),
            seed.saturate<HSLuv>(1.0)
        )
        for (s in shifted) {
            assertEquals(Linearity.SRGB, s.linearity)
            assertTrue(seed.toVector4().distanceTo(s.toVector4()) < 1E-4)
        }
    }

    @Test
    fun testSaturate1Linear() {
        val seed = ColorRGBa.RED.shade(0.5).toLinear()
        val shifted = listOf(
            seed.saturate<HSV>(1.0),
            seed.saturate<HSLuv>(1.0)
        )
        for (s in shifted) {
            assertEquals(Linearity.LINEAR, s.linearity)
            assertTrue(seed.toVector4().distanceTo(s.toVector4()) < 1E-4)
        }
    }

    @Test
    fun testMixedWith() {
        val seed = ColorRGBa.RED
        val mixed = listOf(
            seed.mixedWith<HSLuv>(ColorRGBa.BLUE, 0.5),
            seed.mixedWith<OKLab>(ColorRGBa.BLUE, 0.5),
            seed.mixedWith<OKHSV>(ColorRGBa.BLUE, 0.5)
        )
    }

}