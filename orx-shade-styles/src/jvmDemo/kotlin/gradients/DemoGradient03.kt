package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.math.Vector2

/**
 * Demonstrates how to create a rainbow-like rotating `conic` gradient in `OKHSV` color space.
 * The gradient consists of ten evenly spaced colors, achieved by shifting the hue of a base color.
 * Since the conic gradient covers 360 degrees, changing the `spreadMethod` does not affect the result.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.shadeStyle = gradient<ColorRGBa> {
                    for (i in 0 .. 10) {
                        stops[i/10.0] = ColorRGBa.RED.shiftHue<OKHSV>(i * 36.0)

                    }
                    spreadMethod = SpreadMethod.REFLECT
                    conic {
                        center = Vector2(0.5, 0.5)
                        angle = 360.0
                        rotation = seconds * 10.0
                    }
                }
                drawer.rectangle(drawer.bounds)
            }
        }
    }
}