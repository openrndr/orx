import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.rgb
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noclear.NoClear
import org.openrndr.math.Polar
import org.openrndr.shape.contour
import kotlin.math.sin

fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        var time = 0.0

        if (System.getProperty("takeScreenshot") == "true") {
            extensions.filterIsInstance<SingleScreenshot>().forEach {
                it.delayFrames = 200
            }
        }

        // ------------------------------------------------------------
        // By default OPENRNDR clears the canvas on each animation
        // frame. NoClear disables that behavior, letting you
        // draw on top of what you drew previously.
        // That's the default in some other frameworks.
        // ------------------------------------------------------------
        extend(NoClear()) {
            // backdrop is optional, and it sets the initial state
            // of the canvas. It can be code generated or an image
            // loaded from disk. In this case we start with dark gray.
            backdrop = { drawer.clear(rgb(0.15)) }
        }

        extend {
            // Draw something. For this demo *what* you draw is not so
            // important, only the fact that it stays on the canvas
            // until you draw something else on top of it.

            drawer.isolated {
                // center the origin
                translate(bounds.center)

                for (i in 0..5) {
                    time += 0.01

                    // Make a list of 4 points rotating around the center at
                    // different speeds
                    val points = List(4) {
                        Polar(
                            time * (15.0 + it * 5),
                            250.0 * sin(time + it * 65)
                        ).cartesian
                    }

                    // Use those 4 points to create a Bézier curve
                    val c = contour {
                        moveTo(points.first())
                        curveTo(points[1], points[2], points.last())
                    }

                    // Draw the curve with increasing hue and lightness modulation
                    fill = null
                    stroke = ColorHSLa(
                        time * 10.0, 0.8,
                        0.5 + 0.2 * sin(time * 3), 0.5
                    ).toRGBa()
                    contour(c)
                }
            }
        }
    }
}
