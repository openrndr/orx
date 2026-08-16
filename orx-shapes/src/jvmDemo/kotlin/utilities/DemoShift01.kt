package utilities

import org.openrndr.application
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.utilities.shift
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val grid = drawer.bounds.grid(5,5, 10.0, 10.0, 10.0, 10.0).flatten()

                for ((index, cell) in grid.withIndex()) {

                    val c = Circle(cell.center, cell.width / 2.0).contour
                    drawer.contour(c.shift(index / 25.0))
                    drawer.circle(c.shift(index / 25.0).segments[0].start, 5.0)
                }

            }
        }
    }
}