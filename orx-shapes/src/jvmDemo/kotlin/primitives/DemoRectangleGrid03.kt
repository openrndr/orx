package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.LIME_GREEN
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.color.presets.YELLOW_GREEN
import org.openrndr.extra.shapes.primitives.bounds
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.get
import org.openrndr.shape.bounds

/**
 * Demonstrates the use of the `Rectangle`'s `get()` method,
 * which can be accessed using the square bracket notation.
 *
 * `get()` takes two `IntRange` arguments and returns a `Rectangle`
 * that covers those cell ranges.
 *
 * This program first creates a grid of rectangles covering the whole
 * window and renders it with white borders.
 *
 * Next, it renders five cell ranges in different colors.
 */
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

            drawer.fill = ColorRGBa.YELLOW.shade(0.4).opacify(0.5)
            drawer.rectangle(grid[1..10, 0..4].bounds)

            drawer.fill = ColorRGBa.MAGENTA.shade(0.5).opacify(0.5)
            drawer.rectangle(grid[5..6, 1].bounds)

            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.rectangle(grid[2..5, 2].bounds)

            drawer.fill = ColorRGBa.CYAN.opacify(0.5)
            drawer.rectangle(grid[6..9, 2].bounds)

            drawer.fill = ColorRGBa.LIME_GREEN.shade(0.5).opacify(0.5)
            drawer.rectangle(grid[5..6, 3].bounds)
        }
    }
}
