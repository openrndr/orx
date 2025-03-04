package composed

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.fills.clip.clip
import org.openrndr.extra.shadestyles.fills.gradients.gradient

/**
 * The main entry point of the application that sets up the visual program.
 *
 * This method creates a graphical program with a 720x720 window and uses a rotating
 * gradient-shaded rectangle as the primary visual element. It demonstrates the use
 * of gradient shading and clipping through a compositional approach.
 *
 * The method performs the following actions:
 * 1. Configures the application window size.
 * 2. Constructs a conic gradient with a rotation of 54 degrees and full circular coverage.
 * 3. Creates a star-shaped clip with configurable sharpness, radius, and number of sides.
 * 4. Combines the gradient and clip into a composite shading style.
 * 5. Defines a program loop where the rectangle with the gradient and clip combination
 *    rotates around the center of the canvas while being redrawn continuously.
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