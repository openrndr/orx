import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.panel.style.defaultStyles

/**
 * Demonstrates using collapsible compartments in the GUI. Each one has a description
 * visible (and clickable) in the panel.
 */
fun main() = application {
    configure {
        width = 720
        height = 450
    }
    program {
        val gui = GUI(defaultStyles = defaultStyles(controlFontSize = 20.0))
        gui.compartmentsCollapsedByDefault = false

        val compartments = List(5) {
            @Description("Section with two variables") object {
                @DoubleParameter("x", 0.0, 720.0)
                var x = Double.uniform(0.0, 720.0)

                @DoubleParameter("y", 0.0, 450.0)
                var y = Double.uniform(0.0, 450.0)
            }
        }
        compartments.forEach {
            gui.add(it)
        }
        extend(gui)
        extend {
            compartments.forEach {
                drawer.circle(it.x, it.y, 50.0)
            }
        }
    }
}