package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.extra.shapes.primitives.grid

fun main() = application {
    // Try changing the resolution. The design will use the available space.
    configure {
        width = 800
        height = 400
    }
    program {
        // By specifying the cell size we make sure the design will
        // contain squares, independently of the window size and its
        // aspect ratio.
        val grid = drawer.bounds.grid(
            50.0, 50.0,
            20.0, 20.0, 20.0, 20.0
        ).flatten()

        val grid2 = grid.map { rect ->
            // Each of these inner grids will occupy the available space
            // in the parent grid cells. Notice how we don't specify cell
            // sizes here but counts instead (between 1 and 3 columns and
            // rows)
            val count = Random.int(1, 4)
            rect.grid(count, count, 5.0, 5.0, 5.0, 5.0).flatten()
        }.flatten().filter { Random.bool(0.5) }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.rectangles(grid)
            drawer.fill = ColorRGBa.BLACK
            drawer.rectangles(grid2)
        }
    }
}
