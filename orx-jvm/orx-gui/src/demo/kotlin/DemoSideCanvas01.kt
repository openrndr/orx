import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.draw


/**
 * A simple demonstration of a GUI for drawing some circles
 */
fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true

    }
    program {
        val gui = GUI(baseColor = ColorRGBa.GRAY.shade(0.25))
        gui.compartmentsCollapsedByDefault = false
        gui.enableSideCanvas = true


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

        gui.canvas?.draw {
            val width = drawer.width
            val height = drawer.height
            drawer.fill = settings.color
            drawer.circle(width/2.0, height/2.0, 100.0)
        }
    }
}