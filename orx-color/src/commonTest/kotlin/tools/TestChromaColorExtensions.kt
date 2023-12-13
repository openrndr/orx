package tools

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.toOKLCHa
import org.openrndr.extra.color.tools.findMaxChroma
import org.openrndr.extra.color.tools.isOutOfGamut
import kotlin.test.Test

class TestChromaColorExtensions {
    @Test
    fun testFindMaxChroma() {

        run {
            val i = ColorRGBa.BLUE.toLCHABa()//.withLuminosity(50.0)
            val maxChroma = i.findMaxChroma()
            println(i.chroma)
            println(maxChroma)
        }
        run {
            val i = ColorRGBa.BLUE.toOKLCHa()//.withLuminosity(50.0)
            val maxChroma = i.findMaxChroma()
            println(ColorRGBa.BLUE.isOutOfGamut)
            println(i.chroma)
            println(maxChroma)
            println(i.toRGBa())
        }

    }


}