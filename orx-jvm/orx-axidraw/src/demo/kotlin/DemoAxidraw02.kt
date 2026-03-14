import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.axidraw.PaperSize
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.shapes.primitives.grid
import kotlin.math.sin

/**
 * Demonstrates:
 * - How to set the window size based on the chosen paper size.
 * - How to specify where to draw the paper in the program window.
 * - How to use a windowed GUI.
 * - How to create a design with a size relative to the paper height
 *
 */
fun main() = application {
    program {
        val axi = Axidraw(
            this, PaperSize.A5.size,
            drawer.bounds.offsetEdges(-5.0)
        )

        val gui = WindowedGUI()
        gui.gui.compartmentsCollapsedByDefault = false
        gui.add(axi)

        val settings = @Description("Main") object {

            @ActionParameter("generate")
            fun newDesign() {
                axi.clear()
                axi.draw {
                    axi.bounds.grid(5, 1, 50.0, 50.0, 5.0, 5.0).flatten().forEach {
                        rectangle(it)
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

            // Animated circle, not part of the design
            drawer.circle(drawer.bounds.center, 50.0 + 20.0 * sin(seconds * 5.0))
        }

        keyboard.keyDown.listen {
            if (it.name == "p") {
                val result = axi.onPlot()
                println("error is: ${result.errorCode}")
            }
            if (it.name == "r") {
                val result = axi.resume()
                println("error is: ${result.errorCode}")
            }
        }

    }
}