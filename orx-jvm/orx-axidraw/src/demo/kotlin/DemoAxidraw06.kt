import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.gui.GUI
import org.openrndr.math.Vector2

/**
 * Demonstrates how to draw markers spaced at 1 cm along the edges
 * of a 200x200 mm paper.
 *
 * The Axidraw class draws the paper edge as a pink line. In this case
 * we draw the paper leaving a 50-pixel margin around the window edges.
 *
 * Since this program draws centimeter marks, we disable the camera
 * to make sure the resulting scale is not messed up.
 *
 * Toggle the UI by pressing F11
 */
fun main() = application {
    configure {
        width = 750
        height = 750
    }
    program {
        val axi = Axidraw(
            this, Vector2(200.0),
            drawer.bounds.offsetEdges(-50.0)
        )
        axi.camera.userInteraction = false

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(axi)

        axi.draw {
            // draw markers every centimeter
            val cm = 10 * axi.oneMm
            val paperTopLeft = axi.bounds.corner
            val paperBottomRight = axi.bounds.position(1.0, 1.0)

            var x = paperTopLeft.x + cm
            var y = paperTopLeft.y + cm
            while (x < paperBottomRight.x) {
                lineSegment(x, y, x, y + cm)
                x += cm
            }

            x = paperBottomRight.x - 2 * cm
            y = paperTopLeft.y + cm
            while (y < paperBottomRight.y) {
                lineSegment(x, y, x + cm, y)
                y += cm
            }
        }

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}