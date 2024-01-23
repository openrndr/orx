package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.grid

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            extend {
                drawer.fill = ColorRGBa.WHITE.opacify(0.25)
                drawer.stroke = ColorRGBa.PINK

                // Notice the negative gutter in this grid. It creates an
                // overlap between the resulting rectangles.
                val grid = drawer.bounds.grid(8, 4, 20.0, 20.0, -20.0, -20.0)
                for (cell in grid.flatten()) {
                    drawer.rectangle(cell)
                    drawer.lineSegment(cell.position(0.0, 0.0), cell.position(1.0, 1.0))
                }
            }
        }
    }
}