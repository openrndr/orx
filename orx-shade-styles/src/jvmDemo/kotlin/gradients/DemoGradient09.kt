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

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = gradient<ColorRGBa> {
                stops[0.0] = ColorRGBa.CRIMSON
                stops[0.7] = ColorRGBa.DODGER_BLUE
                stops[1.0] = ColorRGBa.LIME_GREEN

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