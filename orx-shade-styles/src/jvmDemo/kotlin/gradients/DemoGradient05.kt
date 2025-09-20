package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.math.Vector2

/**
 * Reveals the effect of using quantization on a `conic` gradient.
 * By using a `quantization` of 10 we get 9 color bands.
 *
 * Notice how the center of the `conic` gradient is specified in
 * screen coordinates. To make this possible, we need to set the
 * `fillUnits` to `FillUnits.WORLD`. By default, the center of
 * the gradient coordinates is `Vector2(0.5, 0.5)`.
 *
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