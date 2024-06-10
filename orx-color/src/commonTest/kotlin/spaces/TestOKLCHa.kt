package org.openrndr.extra.color.spaces

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.tools.chroma
import org.openrndr.extra.color.tools.withLuminosity
import kotlin.test.Test

class TestOKLCHa {
    @Test
    fun testLowLuminosity() {
        val c = ColorRGBa.GREEN.withLuminosity<OKLCH>(10.0)
        println(c)
        println(c.chroma<OKLCH>())


        val g = ColorRGBa.GREEN.toOKLCHa()
        val gl10 = g.withLuminosity(10.0)
        val rgb10 = gl10.toRGBa()
        println(gl10)
        println(rgb10)
        println(rgb10.toOKLCHa())

    }
}