import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 *  This program generates a Voronoi diagram within a defined circular area and visualizes it.
 *
 *  The program performs the following:
 * - Defines a circular area and a rectangular bounding frame within the canvas.
 * - Uses Poisson Disk Sampling to generate points within the circular area.
 * - Computes the Delaunay triangulation for the generated points, including equidistant points on the circle boundary.
 * - Derives the Voronoi diagram using the Delaunay triangulation and the bounding frame.
 * - Extracts the cell polygons of the Voronoi diagram.
 * - Renders the Voronoi cell polygons on the canvas, with a pink stroke on a black background.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val circle = Circle(Vector2(400.0), 250.0)
        val frame = drawer.bounds.offsetEdges(-50.0)

        val innerPoints = poissonDiskSampling(drawer.bounds, 30.0)
            .filter { circle.contains(it) }
        val edgePoints = circle.contour.equidistantPositions(40)

        val delaunay = (innerPoints + edgePoints).delaunayTriangulation()
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
