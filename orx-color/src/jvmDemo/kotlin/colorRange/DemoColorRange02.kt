package colorRange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.palettes.colorSequence
import org.openrndr.extra.color.spaces.toHSLUVa

/**
 * Demonstrates how to create a `ColorSequence` containing three colors, one of them in the HSLUV color space.
 *
 * Each color in the sequence is assigned a normalized position: in this program, one at the start (0.0),
 * one in the middle (0.5) and one at the end (1.0).
 *
 * The `ColorSpace.blend()` method is used to get a list with 18 interpolated `ColorRGBa` colors,
 * then those colors are drawn as vertical rectangles covering the whole window.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            val cs = colorSequence(
                0.0 to ColorRGBa.PINK,
                0.5 to ColorRGBa.BLUE,
                1.0 to ColorRGBa.PINK.toHSLUVa() // <-- note this color is in HSLUV
            )

            for (c in cs blend (width / 40)) {
                drawer.fill = c
                drawer.stroke = null
                drawer.rectangle(0.0, 0.0, 40.0, height.toDouble())
                drawer.translate(40.0, 0.0)
            }
        }
    }
}