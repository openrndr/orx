package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.presets.CRIMSON
import org.openrndr.extra.color.presets.DODGER_BLUE
import org.openrndr.extra.color.presets.LIME_GREEN
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shadestyles.fills.patterns.pattern

/**
 * Demonstrates two types of shade styles: `pattern` and `luma`.
 *
 * The `pattern` shade style is used to generate a checkers-pattern.
 *
 * This example also loads and draws an image using the `luma` shade style
 * to map pixel brightnesses to gradient colors. Dark colors are
 * mapped to transparent, revealing the checkers-pattern behind it
 * in parts of the image.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = pattern {
                foregroundColor = ColorRGBa.WHITE
                backgroundColor = ColorRGBa.WHITE.shade(0.75)
                checkers {
                    scale = 72.0
                }
            }

            drawer.rectangle(drawer.bounds.offsetEdges(-10.0))

            drawer.shadeStyle = gradient<ColorRGBa> {
                stops[0.0] = ColorRGBa.CRIMSON.opacify(0.0)
                stops[0.19] = ColorRGBa.CRIMSON.opacify(0.0)
                stops[0.25] = ColorRGBa.DODGER_BLUE.opacify(1.0)
                stops[1.0] = ColorRGBa.LIME_GREEN.opacify(1.0)

                spreadMethod = SpreadMethod.REFLECT
                luma {
                    minLevel = 0.1
                    maxLevel = 0.9
                }
            }
            drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))
        }
    }
}