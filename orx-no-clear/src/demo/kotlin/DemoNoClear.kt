import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.rgb
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noclear.NoClear
import org.openrndr.math.Polar
import org.openrndr.shape.contour
import kotlin.math.sin

suspend fun main() {
    application {
        program {
            var time = 0.0

            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.delayFrames = 120
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            // ------------------------------------------------------------
            // By default OPENRNDR clears the canvas on each animation
            // frame. NoClear disables that behavior, letting you
            // draw on top of what you drew previously.
            // That's the default in some other frameworks.
            // ------------------------------------------------------------
            extend(NoClear()) {
                // backdrop is optional and it sets the initial state
                // of the canvas. It can be a generative pattern, an image
                // loaded from disk... In this case we start with dark gray.
                backdrop = { drawer.clear(rgb(0.15)) }
            }

            extend {
                // Draw something. For this demo *what* you draw is not so
                // important, only the fact that it stays on the canvas
                // until you draw something else on top of it.

                drawer.isolated {
                    // center the origin
                    translate(bounds.center)

                    for(i in 0..5) {
                        time += 0.01

                        // Make a list of 4 points rotating around the center at
                        // different speeds
                        val points = List(4) {
                            Polar(time * (15.0 + it * 5),
                                    250.0 * sin(time + it * 65)).cartesian
                        }

                        // Use those 4 points to create a bezier curve
                        val c = contour {
                            moveTo(points.first())
                            curveTo(points[1], points[2], points.last())
                        }

                        // Draw the curve with increasing hue and lightness modulation
                        fill = null
                        stroke = ColorHSLa(time * 10.0, 0.8,
                                0.5 + 0.2 * sin(time * 3), 0.5).toRGBa()
                        contour(c)
                    }
                }
            }
        }
    }
}
