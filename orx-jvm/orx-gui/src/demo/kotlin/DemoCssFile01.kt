import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2

/**
 * Demonstrates a theme for the GUI in a CSS file, `demo-data/css/gui-theme.css`: it sets the custom properties of the
 * default styles and of the GUI, like `--gui-base` and `--control-background`, and adds rules of its own. The file is
 * read again when it changes, edit and save it while the demo runs to restyle the GUI.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.cssFile("demo-data/css/gui-theme.css")

        val settings = @Description("Circle") object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 100.0

            @Vector2Parameter("position", 0.0, 1.0)
            var position = Vector2(0.6, 0.5)

            @ColorParameter("color")
            var color = ColorRGBa.fromHex("#7fd1b9")

            @BooleanParameter("outline")
            var outline = true
        }
        gui.add(settings)
        extend(gui)
        extend {
            drawer.clear(ColorRGBa.fromHex("#14161b"))
            drawer.fill = settings.color
            drawer.stroke = if (settings.outline) ColorRGBa.WHITE else null
            drawer.circle(settings.position * drawer.bounds.dimensions, settings.radius)
        }
    }
}
