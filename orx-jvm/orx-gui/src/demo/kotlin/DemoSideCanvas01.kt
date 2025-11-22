import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.panel.elements.draw


/**
 * Demonstrates the `GUI.enableSideCanvas` feature.
 *
 * When set to true, the `GUI` provides a `canvas` property where one can draw.
 * The size of this canvas is the window size minus the GUI size.
 *
 * That's why if we draw a circle at `drawer.width / 2.0` it is centered
 * on the `canvas`, not on the window.
 *
 * This demo sets the window to resizable, so if you resize the window
 * you should see tha the circle stays at the center of the canvas.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        windowResizable = true
    }

    program {
        val gui = GUI(GUIAppearance(baseColor = ColorRGBa.GRAY.shade(0.25)))
        gui.compartmentsCollapsedByDefault = false
        gui.enableSideCanvas = true

        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 50.0

            @ColorParameter("color")
            var color = ColorRGBa.PINK
        }
        gui.add(settings)
        extend(gui)

        gui.canvas?.draw {
            val width = drawer.width
            val height = drawer.height
            drawer.fill = settings.color
            drawer.circle(width / 2.0, height / 2.0, settings.radius)
        }
    }
}