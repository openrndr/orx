import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.triangulation.Delaunay
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
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val circle = Circle(Vector2(400.0), 250.0)

            val points = poissonDiskSampling(drawer.bounds, 30.0)
                .filter { circle.contains(it) }

            val delaunay = Delaunay.from(points + circle.contour.equidistantPositions(40))
            val triangles = delaunay.triangles().map { it.contour }

            extend {
                drawer.clear(ColorRGBa.BLACK)


                for ((i, triangle) in triangles.withIndex()) {
                    drawer.fill = ColorRGBa.PINK.shade(1.0 - i / (triangles.size * 1.2))
                    drawer.stroke = ColorRGBa.PINK.shade( i / (triangles.size * 1.0) + 0.1)

                    drawer.contour(triangle)
                }
            }
        }
    }
}