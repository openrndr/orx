import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
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

            @DoubleListParameter("a double list")
            var adl = MutableList(2) { 0.0 }

        }

        gui.add(settings)
        extend(gui)
        extend {
            drawer.fill = settings.color
            drawer.circle(settings.position * drawer.bounds.position(1.0, 1.0), settings.radius)
        }
    }
}