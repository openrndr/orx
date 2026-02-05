import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_CYAN
import org.openrndr.extra.color.presets.DARK_ORCHID
import org.openrndr.extra.color.presets.YELLOW_GREEN
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.defaultStyles
import org.openrndr.shape.Circle

/**
 * Demonstrates how to customize the appearance of the GUI by using
 * `GUIAppearance()` and `defaultStyles()`.
 *
 * This demo not only changes the background color and the panel width,
 * but also the widget colors and font size.
 */
fun main() = application {
    configure { height = width }
    program {
        val gui = GUI(
            GUIAppearance(ColorRGBa.DARK_CYAN.shade(0.2).opacify(0.9), 300),
            defaultStyles(
                controlBackground = ColorRGBa.DARK_ORCHID.shade(0.7),
                controlHoverBackground = ColorRGBa.DARK_ORCHID,
                controlTextColor = Color.RGBa(ColorRGBa.YELLOW),
                controlActiveColor = Color.RGBa(ColorRGBa.CYAN),
                controlFontSize = 20.0
            )
        )
        gui.compartmentsCollapsedByDefault = false

        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 100.0, 2, 10)
            var radius = 50.0

            @IntParameter("count", 1, 6, 10)
            var count = 6

            @Vector2Parameter("position", 0.0, 1.0, 1, 20)
            var position = Vector2(0.6, 0.5)

            @DoubleListParameter("radii", 5.0, 30.0, order = 30)
            var radii = mutableListOf(5.0, 6.0, 8.0, 14.0, 20.0, 30.0)

            // Use the mouse wheel when picking a color to control saturation
            @ColorParameter("color", 40)
            var color = ColorRGBa.YELLOW_GREEN

            @ActionParameter("jump")
            fun jump() {
                position = Vector2.uniform(0.0, 1.0)
            }
        }
        gui.add(settings)
        extend(gui)

        // note we can only change the visibility after the extend
        gui.visible = true

        extend {
            // determine visibility through mouse x-coordinate
            //gui.visible = mouse.position.x < gui.appearance.barWidth

            drawer.clear(ColorRGBa.PINK)
            drawer.fill = settings.color
            drawer.circle(settings.position * drawer.bounds.position(1.0, 1.0), settings.radius)
            drawer.circles(
                settings.radii.mapIndexed { i, radius ->
                    Circle(width - 50.0, 60.0 + i * 70.0, radius)
                }.take(settings.count)
            )
        }
    }
}