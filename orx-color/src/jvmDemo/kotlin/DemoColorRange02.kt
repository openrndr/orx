// Create a colorSequence with multiple color models

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.color.palettes.colorSequence
import org.openrndr.extra.color.palettes.rangeTo
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.color.spaces.toHSLUVa

fun main() = application {
    program {
        extend {
            val cs = colorSequence(0.0 to ColorRGBa.PINK,
                    0.5 to ColorRGBa.BLUE,
                    1.0 to ColorRGBa.PINK.toHSLUVa()) // <-- note this one is in hsluv

            for (c in cs blend (width / 40)) {
                drawer.fill = c
                drawer.stroke = null
                drawer.rectangle(0.0, 0.0, 40.0,  height.toDouble())
                drawer.translate(40.0, 0.0)
            }
        }
    }
}