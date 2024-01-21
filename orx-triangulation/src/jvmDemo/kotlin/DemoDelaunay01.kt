import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 800
            height = 800
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
}