import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.shape.Circle

/**
 * A demo rendering four layers including a Voronoi diagram and a Delaunay triangulation,
 * producing a complex pattern.
 *
 * A 8x8 grid of rectangles is produced, leaving a 50 pixel margin around the bounds of the window.
 * Those rectangles are mapped to circles, and each circle contour sampled in 6 locations.
 * This is the set of points used for the Delaunay triangulation.
 *
 * Next, the four layers are rendered:
 *
 * 1. A white dot for each point in the set.
 * 2. Pink contours for the Delaunay half edges.
 * 3. Yellow contours with a Voronoi diagram discarding the ones touching the edges
 * 4. Gray contours with the Delaunay triangles.
 *
 * The structure is recalculated on every animation frame, making it easy
 * to animate some of the parameters.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val r = drawer.bounds.offsetEdges(-50.0)
            val grid = r.grid(8, 8).flatten()
            val circles = grid.map {
                Circle(it.center, it.width / 4.0)
            }
            val points = circles.flatMap { it.contour.equidistantPositions(6) }
            val d = points.delaunayTriangulation()

            drawer.circles(points, 5.0)

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
