import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val frame = Rectangle.fromCenter(Vector2(400.0), 600.0, 600.0)
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
}