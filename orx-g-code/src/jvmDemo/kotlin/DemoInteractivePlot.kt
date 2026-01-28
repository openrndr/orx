import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gcode.BasicGrblGenerator
import org.openrndr.extra.gcode.Origin
import org.openrndr.extra.gcode.Plot
import org.openrndr.math.Vector2
import org.openrndr.shape.ContourBuilder
import org.openrndr.shape.clamp

/**
 * This demo shows how to use the [Plot] class to draw using user input and render the result to G-code.
 *
 * You can use the mouse drag to draw contours on the plot.
 *
 * The input handling code shows how to convert mouse coordinates from the screen space to the document space.
 *
 * Pressing the `g` key will render the g-code and write it to `/tmp`.
 */
fun main() = application {
    configure {
        width = 600
        height = 800
    }

    program {
        extend(Screenshots())

        val plot = Plot(
            dimensions = Vector2(210.0, 297.0), // A4 Portrait
            manualRedraw = false,
            origin = Origin.CENTER
        )
        extend(plot) {
            generator = BasicGrblGenerator()

            // Set output files to be exported to /tmp
            // Press "g" t export g-code.
            folder = "/tmp"

            draw {
                rectangle(docBounds.offsetEdges(-9.0))
                println(docBounds)
            }
        }

        val drawingArea = plot.docBounds.offsetEdges(-10.0)

        val cb = ContourBuilder(true)

        // Handle mouse events and restrict drawing to the drawing area
        mouse.buttonDown.listen {
            val p = plot.toDocumentSpace(it.position)
            cb.moveTo(p.clamp(drawingArea))
        }
        mouse.dragged.listen {
            val p = plot.toDocumentSpace(it.position)
            cb.moveOrLineTo(p.clamp(drawingArea))
        }

        // Draw contours of the contour builder on every frame
        extend {
            plot.layer("drawing") {
                strokeWeight = .5
                contours(cb.result)
            }
        }
    }
}