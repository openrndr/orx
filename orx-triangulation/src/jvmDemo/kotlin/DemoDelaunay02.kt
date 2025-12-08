import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation

/**
 * Demonstrates the `DelaunayTriangulation.halfedges()` method,
 * which returns the boundaries between the triangles in the set.
 *
 * Commented out one can also discover the `hull()` method,
 * which returns a ShapeContour of a convex hull containing
 * all the points in the set.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val frame = drawer.bounds.offsetEdges(-50.0)
        val points = frame.scatter(50.0)

        val delaunay = points.delaunayTriangulation()
        val halfedges = delaunay.halfedges()

        //val hull = delaunay.hull()

        extend {
            drawer.clear(ColorRGBa.BLACK)

            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.contours(halfedges)

            //drawer.stroke = ColorRGBa.GREEN
            //drawer.contour(hull)
        }
    }
}
