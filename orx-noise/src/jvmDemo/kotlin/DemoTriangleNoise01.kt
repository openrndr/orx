import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.shapes.hash
import org.openrndr.shape.Triangle

/**
 * Demonstrate the generation of uniformly distributed points inside a list of triangles.
 * For demonstration purposes there is only one triangle in the list, but could contain many.
 *
 * We can consider the `hash` function as giving us access to a slice in a pool of random Vector2 values.
 * Since we increase the x argument in the call to `hash()` based on the current time in seconds,
 * older random points get replaced by newer ones, then stay visible for a while.
 *
 * @see <img src="https://raw.githubusercontent.com/openrndr/orx/media/orx-noise/images/DemoTriangleNoise01Kt.png">
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val r = drawer.bounds.offsetEdges(-100.0)
        val triangle = Triangle(r.position(0.5, 0.0), r.position(0.0, 1.0), r.position(1.0, 1.0))
        //val pts = listOf(triangle).uniform(1000, Random(0))

        extend {
            val pts = listOf(triangle).hash(1000, 0, (seconds * 500.0).toInt())
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = null
            drawer.contour(triangle.contour)
            drawer.fill = ColorRGBa.BLACK

            drawer.circles(pts, 5.0)
        }
    }
}
