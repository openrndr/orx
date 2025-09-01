import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation

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
