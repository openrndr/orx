import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.math.Vector3
import org.openrndr.shape.LineSegment
import kotlin.random.Random

/**
 * Demonstrates
 * [Shape.constrainedDelaunayTriangulation](https://orx.openrndr.org/orx-triangulation/org.openrndr.extra.triangulation/constrained-delaunay-triangulation.html).
 *
 * The program first creates a list of scattered points sorted via `hilbertOrder`, then connects those points using
 * a Hobby curve. By sorting the points this way, the resulting curve does not cross itself.
 *
 * Next, it calculates a Delaunay triangulation and a Voronoi diagram for it.
 *
 * The program then draws the hobby curve, the Voronoi cells, and the internal Voronoi edges in RED, revealing the
 * inner contours of the blob.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val pts = drawer.bounds.scatter(46.0, random = Random(10)).hilbertOrder()
            val hcog = hobbyCurve(pts, true)
            val hc = hcog.sampleLinear(4.0)

            val dt = hc.shape.constrainedDelaunayTriangulation()
            val vd = dt.voronoiDiagram()

            val ie = vd.internalEdges()

            val vertices = ie.first
            val edges = ie.second

            val lineSegments = edges.map { LineSegment(vertices[it[0]], vertices[it[1]]) }
            extend {
                //val r = Random(1)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(hcog)

                // Draw all Voronoi cells
                for (cell in vd.cellPolygons()) {
                    if (cell.segments.isEmpty()) continue
                    //drawer.fill = ColorRGBa.fromVector(Vector3.uniform(0.0, 1.0, r))
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                    drawer.contour(cell)
                }

                // Draw Voronoi internal edges
                drawer.stroke = ColorRGBa.RED
                drawer.lineSegments(lineSegments)
            }
        }
    }
}