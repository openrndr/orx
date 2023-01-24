import org.openrndr.application
import org.openrndr.extra.hashgrid.filter
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
            val points = (0 until 10000).map {
                drawer.bounds.uniform(random = r)
            }
            val filteredPoints = points.filter(20.0)
            extend {
                drawer.circles(filteredPoints, 4.0)
            }
        }
    }
}