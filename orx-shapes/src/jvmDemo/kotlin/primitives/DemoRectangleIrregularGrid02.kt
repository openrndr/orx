package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.shapes.primitives.column
import org.openrndr.extra.shapes.primitives.irregularGrid
import org.openrndr.extra.shapes.primitives.row
import kotlin.random.Random

/**
 * Demonstrates how to use `Rectangle.irregularGrid()` to create a grid with varying column widths
 * and row heights. The widths and heights are specified as a list of 13 `Double` values, each
 * picked randomly between the values 1.0 and 4.0. This produces two types of columns and two
 * types of rows only: wide ones and narrow ones.
 *
 * The program also demonstrates how to query a `row()` and a `column()` from a `RectangleGrid` instance,
 * both of which return a `List<Rectangle>`. Both `Rectangle` lists are rendered with translucent
 * colors, which makes the intersection of the column and the row slightly brighter.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val r = Random(100)
            val grid = drawer.bounds.irregularGrid(
                List(13) { listOf(1.0, 4.0).random(r) },
                List(13) { listOf(1.0, 4.0).random(r) }
            )

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.rectangles(grid.flatten())

            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.rectangles(grid.column(2))

            drawer.fill = ColorRGBa.CORAL.opacify(0.5)
            drawer.rectangles(grid.row(6))
        }
    }
}