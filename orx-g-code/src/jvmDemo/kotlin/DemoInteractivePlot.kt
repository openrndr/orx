import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.gcode.Origin
import org.openrndr.extra.gcode.Plot
import org.openrndr.extra.gcode.basicGrblSetup
import org.openrndr.math.Vector2
import org.openrndr.shape.ContourBuilder

fun main() = application {
    configure {
        width = 600
        height = 800
    }

    program {
        extend(Screenshots())

        val plot = Plot(
            dimensions = Vector2(210.0, 297.0),
            manualRedraw = false,
            origin = Origin.CENTER
        )
        extend(plot) {
            generator = basicGrblSetup()

            // Set output files to be exported to tmp
            // "g" to export g-code.
            folder = "/tmp"

            draw {
                rectangle(docBounds.offsetEdges(-9.0))
                println(docBounds)
            }
        }

        val drawingArea = plot.docBounds.offsetEdges(-10.0)

        val cb = ContourBuilder(true)

        // Handle mouse events and restrict drawing to drawing area
        mouse.buttonDown.listen {
            val p = plot.toDocumentSpace(it.position)
            if (drawingArea.contains(p)) {
                cb.moveTo(p)
            } else {
                cb.moveTo(drawingArea.contour.nearest(p).position)
            }
        }
        mouse.dragged.listen {
            val p = plot.toDocumentSpace(it.position)
            if (drawingArea.contains(p)) {
                cb.moveOrLineTo(p)
            } else {
                cb.moveOrLineTo(drawingArea.contour.nearest(p).position)
            }
        }

        // Draw contours of contour builder on every frame
        extend {
            plot.layer("drawing") {
                strokeWeight = .5
                contours(cb.result)
            }
        }
    }
}