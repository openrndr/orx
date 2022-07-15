import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val r = drawer.bounds.offsetEdges(-50.0)
                val grid = r.grid(8,8).flatten()
                val circles = grid.map { Circle(it.center, it.width/4.0) }
                val points = circles.flatMap { it.contour.equidistantPositions(6) }
                drawer.circles(points, 5.0)
                val d = Delaunay.from(points)
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(d.halfedges())

                drawer.stroke = ColorRGBa.YELLOW
                drawer.fill = null
                drawer.contours(d.voronoi(drawer.bounds.offsetEdges(-50.0)).cellsPolygons())

                drawer.stroke = ColorRGBa.GRAY
                d.triangles.toList().windowed(3, 3, false).forEach {
                    drawer.lineLoop(listOf(
                    points[it[0]],
                    points[it[1]],
                    points[it[2]]))
                }
            }
        }
    }
}