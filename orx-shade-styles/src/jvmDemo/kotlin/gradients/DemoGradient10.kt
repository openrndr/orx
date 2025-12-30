package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.shadestyles.fills.gradients.gradient

/**
 * Demonstrates how to query the `parameterTypes` and the `parameterValues` exposed
 * by a `ShadeStyle`. Both are `Map`s indexed by the names of the parameters.
 *
 * Useful when using `ShadeStyle`s made by others to discover what parameters
 * are available for us to adjust.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val shadeStyle = gradient<ColorRGBa> {
            stops[0.0] = ColorRGBa.PINK
            stops[1.0] = ColorRGBa.WHITE
            linear {}
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.BLACK
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
            drawer.text("Current shadeStyle parameters:", 30.0, 50.0)

            shadeStyle.parameterTypes.forEach { (name, type)  ->
                val value = shadeStyle.parameterValues[name]
                drawer.translate(0.0, 24.0)
                drawer.text("> $name: $type = $value", 30.0, 50.0)
            }
        }
    }
}