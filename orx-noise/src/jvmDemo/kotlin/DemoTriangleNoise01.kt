import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.shape.Triangle
import kotlin.random.Random

/**
 * Demonstrate the generation of uniformly distributed points inside a list of triangles
 * @see <img src="https://raw.githubusercontent.com/openrndr/orx/media/orx-noise/images/DemoTriangleNoise01Kt.png">
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val r = drawer.bounds.offsetEdges(-100.0)
            val triangle = Triangle(r.position(0.5, 0.0), r.position(0.0, 1.0), r.position(1.0, 1.0))
            val pts = listOf(triangle).uniform(1000, Random(0))
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.stroke = null
                drawer.contour(triangle.contour)
                drawer.fill = ColorRGBa.BLACK

                drawer.circles(pts, 5.0)
            }
        }
    }
}