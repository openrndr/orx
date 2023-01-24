import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.hashgrid.HashGrid
import org.openrndr.extra.noise.uniform
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val r = Random(0)
            val hashGrid = HashGrid(20.0)
            extend {
                val p = drawer.bounds.uniform(random = r)
                if (hashGrid.isFree(p)) {
                    hashGrid.insert(p)
                }
                drawer.circles(hashGrid.points().map { it.first }.toList(), 4.0)
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangles(hashGrid.cells().map { it.bounds }.toList())
            }
        }
    }
}