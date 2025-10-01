import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.extra.noise.shapes.uniform
import kotlin.random.Random

/**
 * This demo creates a `HashGrid` to manage points in a 2D space.
 * Notice the desired cell size in the HashGrid constructor.
 *
 * On every animation frame, it attempts to insert 100 random points into the HashGrid.
 * When a HashGrid cell is free, a point is inserted.
 *
 * The visual output includes:
 * - Rectangles representing the bounds of the occupied cells in the grid.
 * - Circles representing the generated random points.
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
            drawer.stroke = ColorRGBa.PINK
            drawer.circles(hashGrid.points().map { it.first }.toList(), 36.0)
        }
    }
}
