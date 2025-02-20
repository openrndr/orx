package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.bounds
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.get
import org.openrndr.shape.bounds

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val grid = drawer.bounds.grid(12, 5)

        extend {
            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            drawer.rectangles(grid.flatten())

            drawer.fill = ColorRGBa.GRAY.shade(0.4).opacify(0.5)
            drawer.rectangle(grid[1..10, 0..4].bounds)

            drawer.fill = ColorRGBa.PINK.shade(0.5).opacify(0.5)
            drawer.rectangle(grid[5..6, 1].bounds)

            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.rectangle(grid[2..5, 2].bounds)

            drawer.fill = ColorRGBa.GRAY.opacify(0.5)
            drawer.rectangle(grid[6..9, 2].bounds)

            drawer.fill = ColorRGBa.GRAY.shade(0.5).opacify(0.5)
            drawer.rectangle(grid[5..6, 3].bounds)
        }
    }
}
