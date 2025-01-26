import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.shape.Circle

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val r = drawer.bounds.offsetEdges(-50.0)
            val grid = r.grid(8, 8).flatten()
            val circles = grid.map { Circle(it.center, it.width / 4.0) }
            val points = circles.flatMap { it.contour.equidistantPositions(6) }
            drawer.circles(points, 5.0)
            val d = points.delaunayTriangulation()
            drawer.stroke = ColorRGBa.PINK
            drawer.contours(d.halfedges())

            drawer.stroke = ColorRGBa.YELLOW
            drawer.fill = null
            drawer.contours(d.voronoiDiagram(drawer.bounds.offsetEdges(-50.0)).cellPolygons())

            drawer.stroke = ColorRGBa.GRAY
            drawer.contours(d.triangles().map { it.contour })
        }
    }
}
