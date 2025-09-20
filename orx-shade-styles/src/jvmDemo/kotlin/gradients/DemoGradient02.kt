package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient

/**
 * An application with two animated layers of slightly different stellar shade styles.
 *
 * The bottom layer features a rectangle, while the top layer includes a large text
 * repeated 5 times.
 *
 * The only different between the two shade styles is a minor change in the `levelWarp`
 * function, which is used to alter the gradient's level (its normalized `t` value)
 * based on the current coordinates being processed, and the original level at this location.
 *
 * Without this difference, the shader would look identical, and the text would be invisible.
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
                    stops[0.0] = ColorRGBa.BLUE_STEEL
                    stops[0.75] = ColorRGBa.WHITE
                    stops[0.8] = ColorRGBa.BLACK

                    quantization = 10
                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REFLECT
                    levelWarpFunction = """
                        float levelWarp(vec2 p, float level) {
                            return level + cos(p.x * 0.01 + level) * 0.1;
                        }
                    """.trimIndent()

                    stellar {
                        radius = drawer.bounds.width / 4.0
                        center = drawer.bounds.position(0.5, 0.0)
                        sides = 6
                        sharpness = 0.5
                        rotation = seconds * 36.0
                    }
                }
                drawer.rectangle(drawer.bounds)

                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.BLUE_STEEL
                    stops[0.75] = ColorRGBa.WHITE
                    stops[0.8] = ColorRGBa.BLACK

                    quantization = 10
                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REFLECT
                    levelWarpFunction = """
                        float levelWarp(vec2 p, float level) {
                            return level + 0.1 + cos(p.x * 0.01 + level) * 0.1;
                        }
                    """.trimIndent()

                    stellar {
                        radius = drawer.bounds.width / 4.0
                        center = drawer.bounds.position(0.5, 0.0)
                        sides = 6
                        sharpness = 0.5
                        rotation = seconds * 36.0
                    }
                }
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 196.0)
                for (i in 0 until 5) {
                    drawer.text("Gradient", 0.0, 128.0 + i * drawer.height / 5.0)
                }
            }
        }
    }
}