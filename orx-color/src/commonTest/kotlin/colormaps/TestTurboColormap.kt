package org.openrndr.extra.color.colormaps

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTurboColormap {

    @Test
    fun testTurboColormapVector() {
        assertEquals(turboColormapVector(0.0), Vector3(0.13572138, 0.09140261, 0.1066733))
        assertEquals(turboColormapVector(0.5), Vector3(0.5885220621875007, 0.981864383125, 0.31316869781249856))
        assertEquals(turboColormapVector(1.0), Vector3(0.5658592099999993, 0.05038885999999998, -0.025520659999997974))
        assertEquals(turboColormapVector(-0.1), Vector3(0.13572138, 0.09140261, 0.1066733))
        assertEquals(turboColormapVector(1.1), Vector3(0.5658592099999993, 0.05038885999999998, -0.025520659999997974))
    }

    @Test
    fun testTurboColormap() {
        assertEquals(turboColormap(0.0), ColorRGBa(0.13572138, 0.09140261, 0.1066733, linearity = Linearity.LINEAR))
        assertEquals(turboColormap(0.5), ColorRGBa(0.5885220621875007, 0.981864383125, 0.31316869781249856, linearity = Linearity.LINEAR))
        assertEquals(turboColormap(1.0), ColorRGBa(0.5658592099999993, 0.05038885999999998, -0.025520659999997974, linearity = Linearity.LINEAR))
        assertEquals(turboColormap(-0.1), ColorRGBa(0.13572138, 0.09140261, 0.1066733, linearity = Linearity.LINEAR))
        assertEquals(turboColormap(1.1), ColorRGBa(0.5658592099999993, 0.05038885999999998, -0.025520659999997974, linearity = Linearity.LINEAR))
    }

}
