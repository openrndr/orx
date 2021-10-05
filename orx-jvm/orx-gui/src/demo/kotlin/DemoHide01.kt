import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * A simple demonstration of a GUI for drawing some circles
 */
fun main() = application {
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
            @DoubleParameter("radius", 0.0, 100.0)
            var radius = 50.0

            @Vector2Parameter("position", 0.0, 1.0)
            var position = Vector2(0.6, 0.5)

            @ColorParameter("color")
            var color = ColorRGBa.PINK

            @DoubleListParameter("radii", 5.0, 30.0)
            var radii = mutableListOf(5.0, 6.0, 8.0, 14.0, 20.0, 30.0)
        }
        gui.add(settings)
        extend(gui)

        // note we can only change the visibility after the extend
        gui.visible = false

        extend {
            // determine visibility through mouse x-coordinate
            gui.visible = mouse.position.x < 200.0

            drawer.fill = settings.color
            drawer.circle(settings.position * drawer.bounds.position(1.0, 1.0), settings.radius)
            drawer.circles(
                    settings.radii.mapIndexed { i, radius ->
                        Circle(width - 50.0, 60.0 + i * 70.0, radius)
                    }
            )
        }
    }
}