package org.openrndr.extra.color.colormaps

import io.kotest.matchers.shouldBe
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Vector3
import kotlin.test.Test

class TestSpectralZucconi6Colormap {

    @Test
    fun testSpectralZucconi6Vector() {
        spectralZucconi6Vector(0.0) shouldBe Vector3(0.0, 0.0, 0.026075309353279508)
        spectralZucconi6Vector(0.5) shouldBe Vector3(0.49637374891706215, 0.8472371726323733, 0.18366091774095827)
        spectralZucconi6Vector(1.0) shouldBe Vector3(0.0, 0.0, 0.0)
        spectralZucconi6Vector(-0.1) shouldBe Vector3(0.0, 0.0, 0.0)
        spectralZucconi6Vector(1.1) shouldBe Vector3(0.0, 0.0, 0.0)
    }

    @Test
    fun testSpectralZucconi6() {
        spectralZucconi6(0.0) shouldBe ColorRGBa(0.0, 0.0, 0.026075309353279508, linearity = Linearity.SRGB)
        spectralZucconi6(0.5) shouldBe ColorRGBa(0.49637374891706215, 0.8472371726323733, 0.18366091774095827, linearity = Linearity.SRGB)
        spectralZucconi6(1.0) shouldBe ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.SRGB)
        spectralZucconi6(-0.1) shouldBe ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.SRGB)
        spectralZucconi6(1.1) shouldBe ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.SRGB)
    }

}
