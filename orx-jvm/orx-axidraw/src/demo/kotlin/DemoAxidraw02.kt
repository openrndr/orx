import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description

/**
 * Demonstrates:
 * - How to set the window size based on the chosen paper size.
 * - How to use a windowed GUI.
 *
 */
fun main() = application {
    program {
        val axi = Axidraw(this, PaperSize.A5, PaperOrientation.LANDSCAPE)
        axi.resizeWindow()

        val gui = WindowedGUI()
        gui.add(axi)

        val settings = @Description("Main") object {

            @ActionParameter("generate")
            fun generate() {
                axi.clear()
                axi.draw {
                    repeat(20) {
                        circle(axi.bounds.center, Double.uniform(50.0, 200.0))
                    }
                }
            }
        }
        gui.add(settings)

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}