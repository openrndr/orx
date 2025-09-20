package composed

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.fills.clip.clip
import org.openrndr.extra.shadestyles.fills.gradients.gradient

/**
 * Demonstrates how to combine two shade styles
 * (a conic gradient and a rounded star clipping)
 * by using the `+` operator.
 *
 * The design is animated by applying a rotation transformation matrix
 * based in the `seconds` variable.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val g = gradient<ColorRGBa> {
            stops[0.0] = ColorRGBa.BLACK
            stops[1.0] = ColorRGBa.WHITE
            conic {
                rotation = 54.0
                angle = 360.0
            }
        }

        val c = clip {
            clipInner = -0.1
            clipOuter = 0.095
            star {
                sides = 5
                radius = 0.4
                sharpness = 0.74
            }
        }

        // compose the clip and gradient shade styles into one
        val comp = c + g

        extend {
            drawer.translate(width / 2.0, height / 2.0)
            drawer.rotate(seconds * 10.0)
            drawer.translate(-width / 2.0, -height / 2.0)
            drawer.shadeStyle = comp
            drawer.rectangle(drawer.bounds)
        }
    }
}