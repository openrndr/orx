import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.axidraw.PaperOrientation
import org.openrndr.extra.axidraw.PaperSize
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.shapes.primitives.grid

/**
 * Demonstrates:
 * - How to create a flattened grid of with 24 items
 * - How to apply random colors from a palette to each item.
 * - How to use `groupStrokeColors()` to plot a multi-pen design.
 *
 */
fun main() = application {
    program {
        val axi = Axidraw(this, PaperSize.A5, PaperOrientation.PORTRAIT)
        axi.resizeWindow(100.0)

        val gui = WindowedGUI()
        gui.add(axi)

        val palette = listOf(
            ColorRGBa.RED,
            ColorRGBa.GREEN,
            ColorRGBa.BLUE
        )

        axi.clear()
        axi.draw {
            fill = null
            axi.bounds.grid(4, 6).flatten().forEach {
                stroke = palette.random()
                circle(it.center, 50.0)
            }
        }
        axi.groupStrokeColors()

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}
