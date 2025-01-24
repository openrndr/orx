import org.openrndr.application
import org.openrndr.extra.hashgrid.filter
import org.openrndr.extra.noise.shapes.uniform
import kotlin.random.Random

/** A demo to generate and display filtered random points.
 *
 * The program performs the following steps:
 * - Generates 10,000 random points uniformly distributed within the drawable bounds.
 * - Filters the generated points to enforce a minimum distance of 20.0 units between them.
 * - Visualizes the filtered points as circles with a radius of 10.0 units on the canvas.
 */
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
                drawer.circles(filteredPoints, 10.0)
            }
        }
    }
}