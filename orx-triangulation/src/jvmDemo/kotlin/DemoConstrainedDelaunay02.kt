import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.shape.LineSegment
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val pts = drawer.bounds.scatter(46.0, random = Random(10)).hilbertOrder()
            val hc = hobbyCurve(pts, true).sampleLinear(4.0)

            val dt = hc.shape.constrainedDelaunayTriangulation()
            val vd = dt.voronoiDiagram()

            val ie = vd.internalEdges()

            val vertices = ie.first
            val edges = ie.second

            val lineSegments = edges.map { LineSegment(vertices[it[0]], vertices[it[1]]) }
            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(hc)

                for (cell in vd.cellPolygons()) {
                    if (cell.segments.isEmpty()) continue
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                    drawer.contour(cell)
                }

                drawer.stroke = ColorRGBa.RED
                drawer.lineSegments(lineSegments)
            }
        }
    }
}