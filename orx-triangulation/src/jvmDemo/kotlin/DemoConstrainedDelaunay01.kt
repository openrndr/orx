import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.remotecontrol.RemoteControl
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizes a constrained Delaunay triangulation of a non-convex (star-shaped) outer
 * contour with two rectangular holes, to confirm no triangles leak outside the shape
 * or into the holes.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend(RemoteControl())

        val center = Vector2(360.0, 360.0)
        val starPoints = (0 until 36).map { i ->
            val r = if (i % 2 == 0) 300.0 else 160.0
            val angle = PI * i / 18
            center + Vector2(r * cos(angle), r * sin(angle))
        }
        val outer = ShapeContour.fromPoints(starPoints, closed = true)
        val hole1 = Rectangle(260.0, 260.0, 60.0, 60.0).contour.reversed
        val hole2 = Rectangle(380.0, 380.0, 80.0, 50.0).contour.reversed
        val shape = Shape(listOf(outer, hole1, hole2))

        val pts = shape.scatter(20.0, distanceToEdge = 10.0)
        val cdt = shape.constrainedDelaunayTriangulation(pts)

        extend {
            drawer.clear(ColorRGBa.BLACK)

            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 1.0
            drawer.fill = ColorRGBa.PINK.opacify(0.3)
            drawer.contours(cdt.triangles().map { it.contour })

            drawer.fill = null
            drawer.stroke = ColorRGBa.YELLOW
            drawer.strokeWeight = 2.0
            drawer.contour(outer)
            drawer.contour(hole1)
            drawer.contour(hole2)
        }
    }
}
