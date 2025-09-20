package patterns

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.PEACH_PUFF
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.clip.clip
import org.openrndr.extra.shadestyles.fills.gradients.gradient
import org.openrndr.extra.shadestyles.fills.patterns.pattern
import kotlin.math.cos

/**
 * Demonstrates the use of a complex shade style made by combining an
 * animated `pattern`, a `gradient` and a `clip`.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend(Camera2D())
        extend {
            drawer.shadeStyle = pattern {
                backgroundColor = ColorRGBa.DARK_GRAY
                foregroundColor = ColorRGBa.PEACH_PUFF
                patternUnits = FillUnits.WORLD
                parameter("time", seconds * 0.1)
                scale = 0.2
                crosses {
                    width = 1.0
                    weight = 0.2
                    rotation = seconds * 45.0
                    strokeWeight = cos(seconds) * 0.3 + 0.31
                }
            } + gradient<ColorRGBa> {
                stops[1.0] = ColorRGBa.BLACK
                stops[0.5] = ColorRGBa.WHITE
                stops[0.0] = ColorRGBa.WHITE
                conic { }
            } + clip {
                star {
                    sides = 36
                    sharpness = 0.1
                    clipOuter = 0.05
                    clipInner = -0.1
                    radius = 0.4
                }
            }

            drawer.rectangle(drawer.bounds.offsetEdges(-50.0))

//            drawer.fill = ColorRGBa.WHITE
//            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 196.0)
//            drawer.text("Patterns", 10.0, height / 2.0)


        }
    }
}