import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2

/**
 * A simple demonstration of a GUI for drawing a single circle
 */
fun main() = application {
    program {
        val gui = GUI()
        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 100.0)
            var radius = 50.0

            @Vector2Parameter("position", 0.0, 1.0)
            var position = Vector2.ZERO

            @ColorParameter("color")
            var color = ColorRGBa.PINK

        }
        gui.add(settings)
        extend(gui)
        extend {
            drawer.fill = settings.color
            drawer.circle(settings.position * drawer.bounds.position(1.0, 1.0), settings.radius)
        }
    }
}