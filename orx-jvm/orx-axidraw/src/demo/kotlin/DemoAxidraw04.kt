import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.*
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.shapes.primitives.grid

/**
 * Demonstrates:
 * - How to create a flattened grid of with 24 items
 * - How to randomize the order of those items
 * - How to take chunks of 10 items, then make
 *   a pause to change the pen after plotting each chunk
 * - How to leave a safety margin around the plot to avoid
 *   plotting outside the paper (by setting `axi.margin`
 *   and `axi.occlusion`).
 *
 * Operation: After plotting ten circles, plotting will stop to let you change the pen.
 * With the second pen installed, click `resume`. It will plot ten circles more.
 * Change the pen again and click `resume` to plot the remaining 4 circles.
 * Once done, click `resume` one more time to bring the pen home.
 */
fun main() = application {
    program {
        val axi = Axidraw(this, PaperSize.A5.size, drawer.bounds)

        val gui = WindowedGUI()
        gui.add(axi)

        // By default, the gui state is loaded from disk
        // when the program starts. Use the `gui.loaded` event to override loaded values.
        gui.gui.loaded.listenOnce {
            axi.margin = 15
            axi.occlusion = true
        }

        axi.clear()
        axi.draw {
            fill = null
            axi.bounds.grid(4, 6).flatten()
                .shuffled().chunked(10).forEach { chunk ->
                    group {
                        chunk.forEach {
                            circle(it.center, 50.0)
                        }
                    }
                    group {
                    }.configure(layerMode = AxiLayerMode.PAUSE)
                }
        }

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}