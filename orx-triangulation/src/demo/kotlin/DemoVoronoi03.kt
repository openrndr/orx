import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 750
            height = 1000
        }
        program {
            extend {

                val r = drawer.bounds.offsetEdges(-100.0)
                val grid = r.grid(3,6).flatten()
                val circles = grid.map { Circle(Vector2.ZERO, 158.975).contour.transform(
                    buildTransform {
                        translate(it.center)
                        rotate(Vector3.UNIT_Z, 0.0)
                    }
                ) }
                val points = circles.flatMap { it.contour.equidistantPositions(16).take(16) }
                drawer.circles(points, 5.0)
                val d = Delaunay.from(points)
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(d.halfedges())

                drawer.stroke = ColorRGBa.YELLOW
                drawer.fill = ColorRGBa.GRAY.opacify(0.5)
                drawer.contours(d.voronoi(drawer.bounds).cellsPolygons())

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