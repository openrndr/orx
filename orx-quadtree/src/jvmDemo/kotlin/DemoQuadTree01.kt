import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.rectangleBatch
import org.openrndr.extra.noise.gaussian
import org.openrndr.extra.quadtree.Quadtree
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.random.Random

/**
 * Demonstrates how to create a `QuadTree` data structure,
 * how to add 2D points to it, and how to visualize all the quads
 * created for the current set of points.
 *
 * The demo creates 1000 points using a Gaussian distribution, which
 * creates a higher density of points in the center of the window.
 *
 * The `QuadTree` algorithm tries to keep the number of points
 * per quad balanced, which in this case leads to larger quads
 * near the edges of the window, and small quads at the center.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
        title = "QuadTree"
    }
    program {
        val box = Rectangle.fromCenter(Vector2(400.0), 750.0)
        val rnd = Random(1)

        val points = (0 until 1_000).map {
            Vector2.gaussian(box.center, Vector2(95.0), rnd)
        }

        val quadTree = Quadtree<Vector2>(box) { it }

        for (point in points) {
            quadTree.insert(point)
        }

        val batch = drawer.rectangleBatch {
            this.fill = null
            this.stroke = ColorRGBa.GRAY
            this.strokeWeight = 0.5
            quadTree.batch(this)
        }

        extend {
            drawer.clear(ColorRGBa.BLACK)

            drawer.rectangles(batch)

            drawer.fill = ColorRGBa.PINK.opacify(0.7)
            drawer.stroke = null
            drawer.circles(points, 5.0)
        }
    }
}
