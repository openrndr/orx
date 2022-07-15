import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val frame = Rectangle.fromCenter(Vector2(400.0), 600.0, 600.0)

            val points = poissonDiskSampling(frame, 50.0).map { it + frame.corner }

            val delaunay = Delaunay.from(points)
            val halfedges = delaunay.halfedges()
            val hull = delaunay.hull()

            extend {
                drawer.clear(ColorRGBa.BLACK)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(halfedges)

                drawer.stroke = ColorRGBa.GREEN
                drawer.contour(hull)
            }
        }
    }
}