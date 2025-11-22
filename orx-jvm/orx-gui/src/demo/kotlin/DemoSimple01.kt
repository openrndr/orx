import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * Demonstrates how to create a simple GUI with 4 inputs:
 * - A `ColorParameter` which creates a color picker.
 * - A `DoubleParameter` to control the radius of a circle.
 * - A `Vector2Parameter` to set the position of that circle.
 * - A `DoubleListParameter` which sets the radii of six circles.
 *
 * The demo also shows how to use the variables controlled by the GUI
 * inside the program, so changes to those variables affect
 * the rendering in real time.
 */
fun main() = application {
    configure {
        width = 720
        height = 450
    }
    program {
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
        extend {
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