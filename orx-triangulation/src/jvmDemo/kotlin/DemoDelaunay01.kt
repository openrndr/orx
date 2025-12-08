import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * This demo shows how to use Delaunay triangulation to convert a Shape into a list of triangular ShapeContours.
 *
 * The program starts by creating a Circle, then creates two sets of points:
 * - Points generated within the circle using a scatter algorithm that
 *   maintains specific spacing and avoids clustering.
 * - Points sampled along the contour of the circle.
 *
 * The `delaunayTriangulation()` method is called on the combined point set.
 * Next, it queries the resulting triangles and converts them into ShapeContour
 * instances.
 *
 * Finally, it renders the triangles assigning unique fill and stroke colors
 * based on the triangle's index.
 *
 * This method demonstrates concepts of computational geometry and procedural
 * rendering.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        title = "Delaunator"
    }
    program {
        val circle = Circle(Vector2(400.0), 250.0)

        val innerPoints = circle.shape.scatter(30.0)
        val edgePoints = circle.contour.equidistantPositions(40)

        val delaunay = (innerPoints + edgePoints).delaunayTriangulation()
        val triangles = delaunay.triangles().map { it.contour }

        extend {
            drawer.clear(ColorRGBa.BLACK)
            for ((i, triangle) in triangles.withIndex()) {
                drawer.fill = ColorRGBa.PINK.shade(1.0 - i / (triangles.size * 1.2))
                drawer.stroke = ColorRGBa.PINK.shade(i / (triangles.size * 1.0) + 0.1)
                drawer.contour(triangle)
            }
        }
    }
}
