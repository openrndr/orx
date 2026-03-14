import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.axidraw.PaperSize
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.shapes.primitives.grid

/**
 * Demonstrates:
 * - How to create a flattened grid of with 24 items
 * - How to apply random colors from a palette to each item.
 * - How to use `groupStrokeColors()` to plot a multi-pen design
 *   and visualize the color order in the program window.
 *
 */
fun main() = application {
    configure {
        width = 1000
        height = 1500
    }
    program {
        val axi = Axidraw(
            this,
            PaperSize.A5.size.yx,
            drawer.bounds.offsetEdges(-40.0)
        )

        val gui = WindowedGUI()
        gui.add(axi)
        gui.gui.compartmentsCollapsedByDefault = false

        val palette = listOf(
            ColorRGBa.RED,
            ColorRGBa.GREEN,
            ColorRGBa.BLUE
        )

        axi.clear()
        axi.draw {
            fill = null
            axi.bounds.grid(6, 4).flatten().forEach {
                stroke = palette.random()
                circle(it.center, 50.0)
            }
        }
        val penColors = axi.groupStrokeColors()

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)

            // Display pen colors so you know which pen to install next
            penColors.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.rectangle(50.0 + i * 50.0, height - 50.0, 40.0, 40.0)
            }
        }
    }
}
