import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.extra.noise.shapes.uniform
import kotlin.random.Random

/**
 * This demo sets up an interactive graphics application with a configurable
 * display window and visualization logic. It uses a `HashGrid` to manage points
 * in a 2D space and randomly generates points within the drawable area. These
 * points are then inserted into the grid if they satisfy certain spatial conditions.
 * The visual output includes:
 * - Rectangles representing the bounds of the cells in the grid.
 * - Circles representing the generated points.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val r = Random(0)
        val hashGrid = HashGrid(72.0)

        extend {
            for (i in 0 until 100) {
                val p = drawer.bounds.uniform(random = r)
                if (hashGrid.isFree(p)) {
                    hashGrid.insert(p)
                }
            }

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.rectangles(hashGrid.cells().map { it.bounds }.toList())
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.circles(hashGrid.points().map { it.first }.toList(), 36.0)
        }
    }
}
