import org.openrndr.application
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.parameters.DoubleParameter
import kotlin.system.exitProcess

/**
 * Demonstration of multi window GUI using WindowedGUI extension
 */
fun main() {
    // skip this demo on CI
    if (System.getProperty("takeScreenshot") == "true") {
        exitProcess(0)
    }
    application {
        program {
            val settings = object {
                @DoubleParameter("radius", 10.0, 100.0)
                var radius = 10.0
            }
            val gui = WindowedGUI()
            gui.add(settings)
            extend(gui)

            extend {
                drawer.circle(drawer.bounds.center, settings.radius)
            }
        }
    }
}