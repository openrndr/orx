import org.openrndr.MouseEvent
import org.openrndr.application
import org.openrndr.extensions.Screenshots
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

        val plot = Plot(dimensions = Vector2(210.0, 297.0), manualRedraw = false)
        extend(plot) {
            generator = basicGrblSetup()

            // Set output files to be exported to tmp
            // "g" to export g-code.
            folder = "/tmp"
        }

        val drawingArea = plot.docBounds.offsetEdges(-10.0)

        // Converts mouse events position to document space
        fun MouseEvent.documentPosition(): Vector2 {
            val s = 1 / plot.scale()
            return Vector2(position.x * s, plot.docBounds.height - position.y * s)
        }


        val cb = ContourBuilder(true)

        // Handle mouse events and restrict drawing to drawing area
        mouse.buttonDown.listen {
            val p = it.documentPosition()
            if (drawingArea.contains(p)) {
                cb.moveTo(p)
            } else {
                cb.moveTo(drawingArea.contour.nearest(p).position)
            }
        }
        mouse.dragged.listen {
            val p = it.documentPosition()
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