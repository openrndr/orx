import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.MiniGUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2

/**
 * Demonstrates [MiniGUI], a compact GUI that shows a single horizontal bar at the bottom of the screen.
 *
 * The dropdown on the left of the bar selects the compartment to show, the controls of the selected compartment are
 * laid out horizontally next to it.
 */
fun main() = application {
    configure {
        width = 960
        height = 540
    }
    program {
        val gui = MiniGUI()

        val circle = @Description("Circle") object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 80.0

            @XYParameter("position", 0.0, 1.0, 0.0, 1.0)
            var position = Vector2(0.5, 0.5)

            @ColorParameter("color")
            var color = ColorRGBa.PINK

            @BooleanParameter("stroke")
            var stroke = true
        }

        val grid = @Description("Grid") object {
            @IntParameter("count", 1, 32)
            var count = 8

            @DoubleParameter("thickness", 0.5, 4.0)
            var thickness = 1.0

            @ColorParameter("color")
            var color = ColorRGBa.GRAY
        }

        val background = @Description("Background") object {
            @ColorParameter("color")
            var color = ColorRGBa.BLACK

            @ActionParameter("invert")
            fun invert() {
                color = ColorRGBa(1.0 - color.r, 1.0 - color.g, 1.0 - color.b, color.alpha)
            }
        }

        gui.add(circle)
        gui.add(grid)
        gui.add(background)
        extend(gui)

        extend {
            drawer.clear(background.color)
            drawer.stroke = grid.color
            drawer.strokeWeight = grid.thickness
            for (i in 1 until grid.count) {
                val x = width * i / grid.count.toDouble()
                val y = height * i / grid.count.toDouble()
                drawer.lineSegment(x, 0.0, x, height.toDouble())
                drawer.lineSegment(0.0, y, width.toDouble(), y)
            }
            drawer.fill = circle.color
            drawer.stroke = if (circle.stroke) ColorRGBa.WHITE else null
            drawer.strokeWeight = 2.0
            drawer.circle(circle.position * drawer.bounds.position(1.0, 1.0), circle.radius)
        }
    }
}
