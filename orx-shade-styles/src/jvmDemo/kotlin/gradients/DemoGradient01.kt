package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.math.Vector2
import kotlin.math.cos

/**
 * Demonstrates how to create 4 animated gradient shade-styles with 5 colors:
 * - a linear gradient
 * - a stellar gradient
 * - a radial gradient
 * - a linear gradient with `SpreadMethod.REPEAT`
 * Each gradient style has different adjustable attributes.
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
                    stops[0.0] = ColorRGBa.RED
                    stops[0.1] = ColorRGBa.GREEN
                    stops[0.2] = ColorRGBa.PINK
                    stops[0.9] = ColorRGBa.WHITE
                    stops[1.0] = ColorRGBa.BLACK

                    linear {
                        start = Vector2(0.1, 0.1).rotate(seconds * 36.0, Vector2(0.5, 0.5))
                        end = Vector2(0.9, 0.9).rotate(seconds * 36.0, Vector2(0.5, 0.5))
                    }
                }
                drawer.rectangle(0.0, 0.0, 360.0, 360.0)


                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.RED
                    stops[0.1] = ColorRGBa.GREEN
                    stops[0.2] = ColorRGBa.PINK
                    stops[0.9] = ColorRGBa.WHITE
                    stops[1.0] = ColorRGBa.BLACK
                    spreadMethod = SpreadMethod.REFLECT
                    stellar {
                        radius = (cos(seconds) * 0.25 + 0.5) * 0.5
                        sharpness = 0.5
                        sides = 6
                        rotation = seconds * 36.0
                    }
                }
                drawer.rectangle(360.0, 0.0, 360.0, 360.0)

                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.RED
                    stops[0.1] = ColorRGBa.GREEN
                    stops[0.2] = ColorRGBa.PINK
                    stops[0.9] = ColorRGBa.WHITE
                    stops[1.0] = ColorRGBa.BLACK
                    spreadMethod = SpreadMethod.REFLECT
                    radial {
                        radius = (cos(seconds) * 0.25 + 0.5) * 0.5
                    }
                }
                drawer.rectangle(360.0, 360.0, 360.0, 360.0)


                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.RED
                    stops[0.1] = ColorRGBa.GREEN
                    stops[0.2] = ColorRGBa.PINK
                    stops[0.9] = ColorRGBa.WHITE
                    stops[1.0] = ColorRGBa.BLACK
                    spreadMethod = SpreadMethod.REPEAT
                    linear {
                        start = Vector2(0.45, 0.45).rotate(seconds * 36.0)
                        end = Vector2(0.55, 0.55).rotate(seconds * 36.0)
                    }
                }
                drawer.rectangle(0.0, 360.0, 360.0, 360.0)
            }
        }
    }
}