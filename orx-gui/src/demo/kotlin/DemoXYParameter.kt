import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false

        val settings = @Description("Settings") object {
            @XYParameter("Position", 0.0, 800.0, 0.0, 800.0,
                    precision = 2,
                    invertY = true,
                    showVector = true)
            var position: Vector2 = Vector2(0.0,0.0)
        }

        gui.add(settings)

        extend(gui)
        extend {
            drawer.circle(settings.position, 50.0)
        }
    }
}
