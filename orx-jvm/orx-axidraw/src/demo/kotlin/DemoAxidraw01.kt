import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter
import kotlin.math.min

/**
 * Demonstrates:
 * - how to create an AxiDraw GUI
 * - how to add a slider and a button to that GUI
 * - how to include code to generate new random designs that match
 *   the paper size via `axi.bounds`.
 * - how to display the generated design using `axi.display`.
 *
 * Toggle the GUI by pressing F11.
 */
fun main() = application {
    configure {
        width = PaperSize.A5.size.x * 5
        height = PaperSize.A5.size.y * 5
    }
    program {
        val axi = Axidraw(PaperSize.A5)

        val gui = GUI()
        gui.add(axi)

        val settings = @Description("Main") object {
            @IntParameter("count", 1, 50)
            var count = 20

            @ActionParameter("generate")
            fun generate() {
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

        settings.generate()

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}