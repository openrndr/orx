package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.BLACK
                    stops[0.5] = ColorRGBa.BLUE_STEEL
                    stops[1.0] = ColorRGBa.WHITE

                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REPEAT

                    quantization = 10
                    conic {
                        angle = 360.0 * 8.0
                        center = drawer.bounds.position(0.5, 0.5)
                    }
                }

                drawer.rectangle(drawer.bounds)

            }
        }
    }
}