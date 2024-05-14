import org.openrndr.WindowConfiguration
import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.window
import kotlin.system.exitProcess

/**
 * Demonstration of multi window GUI in the manual way
 */
fun main() {
    // skip this demo on CI
    if (System.getProperty("takeScreenshot") == "true") {
        exitProcess(0)
    }
    application {
        program {
            val settings = object  {
                @DoubleParameter("radius", 10.0, 100.0)
                var radius = 10.0
            }
            window(WindowConfiguration(width = 200, resizable = true)) {
                val gui = GUI()
                gui.add(settings)
                extend(gui)
            }
            extend {
                drawer.circle(drawer.bounds.center, settings.radius)
            }
        }
    }
}