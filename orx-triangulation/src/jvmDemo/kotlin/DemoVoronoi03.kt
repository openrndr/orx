import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle

/**
 * A variation of DemoVoronoi02.kt, also rendering four layers including
 * a Voronoi diagram and a Delaunay triangulation,
 * producing a complex pattern.
 *
 * A 3x6 grid of rectangles is produced, leaving a 100 pixel margin around the bounds of the window.
 * Those rectangles are mapped to circles, and each circle contour sampled in 16 locations.
 * This is the set of points used for the Delaunay triangulation.
 **
 * Next, four layers are rendered:
 *
 * 1. A white dot for each point in the set.
 * 2. Pink contours for the Delaunay half edges.
 * 3. A Voronoi diagram with yellow contours with translucent fill.
 * 4. Gray contours with the Delaunay triangles.
 *
 * The structure is recalculated on every animation frame, making it easy
 * to animate some of the parameters. Try replacing the 0.0 rotation
 * of the circles by other values or even `seconds` and observe what happens.
 */
fun main() = application {
    configure {
        width = 720
        height = 1000
    }
    program {
        extend {
            val r = drawer.bounds.offsetEdges(-100.0)
            val grid = r.grid(3, 6).flatten()
            val circles = grid.map {
                Circle(Vector2.ZERO, 158.975).contour.transform(
                    buildTransform {
                        translate(it.center)
                        rotate(Vector3.UNIT_Z, 0.0)
                    }
                )
            }
            val points = circles.flatMap { it.contour.equidistantPositions(16) }
            val d = points.delaunayTriangulation()

            drawer.circles(points, 5.0)

            drawer.stroke = ColorRGBa.PINK
            drawer.contours(d.halfedges())

            drawer.stroke = ColorRGBa.YELLOW
            drawer.fill = ColorRGBa.GRAY.opacify(0.5)
            drawer.contours(d.voronoiDiagram(drawer.bounds).cellPolygons())

            drawer.stroke = ColorRGBa.GRAY
            drawer.contours(d.triangles().map { it.contour })
        }
    }
}
