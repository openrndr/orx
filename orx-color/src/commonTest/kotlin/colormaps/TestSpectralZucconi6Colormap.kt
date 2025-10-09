package org.openrndr.extra.color.colormaps

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSpectralZucconi6Colormap {

    @Test
    fun testSpectralZucconi6Vector() {
        assertEquals(spectralZucconi6Vector(0.0) , Vector3(0.0, 0.0, 0.026075309353279508))
        assertEquals(spectralZucconi6Vector(0.5) , Vector3(0.49637374891706215, 0.8472371726323733, 0.18366091774095827))
        assertEquals(spectralZucconi6Vector(1.0) , Vector3(0.0, 0.0, 0.0))
        assertEquals(spectralZucconi6Vector(-0.1) , Vector3(0.0, 0.0, 0.0))
        assertEquals(spectralZucconi6Vector(1.1) , Vector3(0.0, 0.0, 0.0))
    }

    @Test
    fun testSpectralZucconi6() {
        assertEquals(spectralZucconi6(0.0), ColorRGBa(0.0, 0.0, 0.026075309353279508, linearity = Linearity.LINEAR))
        assertEquals(spectralZucconi6(0.5), ColorRGBa(0.49637374891706215, 0.8472371726323733, 0.18366091774095827, linearity = Linearity.LINEAR))
        assertEquals(spectralZucconi6(1.0), ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.LINEAR))
        assertEquals(spectralZucconi6(-0.1), ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.LINEAR))
        assertEquals(spectralZucconi6(1.1), ColorRGBa(0.0, 0.0, 0.0, linearity = Linearity.LINEAR))
    }

}
