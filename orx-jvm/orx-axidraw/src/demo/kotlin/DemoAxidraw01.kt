import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.axidraw.PaperSize
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector2
import kotlin.math.min

/**
 * Demonstrates:
 * - how to instantiate the AxiDraw class and add it to a GUI
 * - how to add a slider and a button to that GUI
 * - how to include code to generate new random designs that match
 *   the paper size via `axi.bounds`.
 * - how to display the generated design using `axi.display`.
 *
 * A Camera2D is enabled by default. You can click and drag your
 * left / right mouse buttons to pan / rotate and use the mouse
 * wheel to zoom in and out. Use the camera to place your design
 * in the paper.
 */
fun main() = application {
    configure {
        width = 750
        height = 750
    }
    program {
        val axi = Axidraw(
            this,
            PaperSize.A4.size,
            drawer.bounds.offsetEdges(-10.0),
            fit = Vector2.UNIT_X
        )
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(axi)

        val settings = @Description("Main") object {
            @IntParameter("count", 1, 50)
            var count = 20

            @ActionParameter("generate")
            fun newDesign() {
                axi.clear()
                axi.draw {
                    val l = min(axi.bounds.width, axi.bounds.height) / 2.0
                    repeat(count) {
                        circle(axi.bounds.center, Double.uniform(l / 4.0, l))
                    }
                }
            }
        }
        gui.add(settings)

        settings.newDesign()

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}