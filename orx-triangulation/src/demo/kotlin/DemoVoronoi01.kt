import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
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

            val circle = Circle(Vector2(400.0), 250.0)
            val frame = Rectangle.fromCenter(Vector2(400.0), 600.0, 600.0)

            val points = poissonDiskSampling(drawer.bounds, 30.0)
                .filter { circle.contains(it) }

            val delaunay = (points + circle.contour.equidistantPositions(40)).delaunayTriangulation()
            val voronoi = delaunay.voronoiDiagram(frame)

            val cells = voronoi.cellPolygons()

            extend {
                drawer.clear(ColorRGBa.BLACK)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(cells)
            }
        }
    }
}