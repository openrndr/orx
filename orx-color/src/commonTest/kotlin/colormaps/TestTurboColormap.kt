package org.openrndr.extra.color.colormaps

import io.kotest.matchers.shouldBe
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Vector3
import kotlin.test.Test

class TestTurboColormap {

    @Test
    fun testTurboColormapVector() {
        turboColormapVector(0.0) shouldBe Vector3(0.13572138, 0.09140261, 0.1066733)
        turboColormapVector(0.5) shouldBe Vector3(0.5885220621875007, 0.981864383125, 0.31316869781249856)
        turboColormapVector(1.0) shouldBe Vector3(0.5658592099999993, 0.05038885999999998, -0.025520659999997974)
        turboColormapVector(-0.1) shouldBe Vector3(0.13572138, 0.09140261, 0.1066733)
        turboColormapVector(1.1) shouldBe Vector3(0.5658592099999993, 0.05038885999999998, -0.025520659999997974)
    }

    @Test
    fun testTurboColormap() {
        turboColormap(0.0) shouldBe ColorRGBa(0.13572138, 0.09140261, 0.1066733, linearity = Linearity.SRGB)
        turboColormap(0.5) shouldBe ColorRGBa(0.5885220621875007, 0.981864383125, 0.31316869781249856, linearity = Linearity.SRGB)
        turboColormap(1.0) shouldBe ColorRGBa(0.5658592099999993, 0.05038885999999998, -0.025520659999997974, linearity = Linearity.SRGB)
        turboColormap(-0.1) shouldBe ColorRGBa(0.13572138, 0.09140261, 0.1066733, linearity = Linearity.SRGB)
        turboColormap(1.1) shouldBe ColorRGBa(0.5658592099999993, 0.05038885999999998, -0.025520659999997974, linearity = Linearity.SRGB)
    }

}
