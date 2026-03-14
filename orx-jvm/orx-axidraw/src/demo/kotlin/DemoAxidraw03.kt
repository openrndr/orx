import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.axidraw.PaperSize
import org.openrndr.extra.axidraw.configure
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid

/**
 * Demonstrates:
 * - How to create layers via `group` and give each layer
 * a unique pen height and pen speed.
 *
 */
fun main() = application {
    program {
        val axi = Axidraw(
            this,
            PaperSize.A5.size.yx,
            drawer.bounds.offsetEdges(-10.0)
        )

        val gui = WindowedGUI()
        gui.add(axi)

        axi.clear()
        axi.draw {
            fill = null
            axi.bounds.grid(6, 4).flatten().forEach {
                group {
                    circle(it.center, axi.bounds.height / 5)
                }.configure(
                    penHeight = Int.uniform(30, 60),
                    penSpeed = Int.uniform(20, 50)
                )
            }
        }

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}