import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * This method sets up a graphical application using the OPENRNDR framework
 * to visually demonstrate Delaunay triangulation on a set of points scattered
 * along a circle with Poisson disk sampling.
 *
 * The application features the following:
 * - A central circle with a defined radius.
 * - Points generated within the circle using a scatter algorithm that
 *   maintains specific spacing and avoids clustering.
 * - Delaunay triangulation computed from the combined point set.
 * - Rendering of triangles that are part of the Delaunay triangulation.
 * - Visual styling with dynamic color shading for better clarity of layers
 *   and triangle order.
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
        val points = circle.shape.scatter(30.0)

        val delaunay = (points + circle.contour.equidistantPositions(40)).delaunayTriangulation()
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
