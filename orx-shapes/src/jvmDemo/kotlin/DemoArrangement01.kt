import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.shapes.Arrangement
import org.openrndr.extra.shapes.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.random.Random

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        // Create some shapes
        val outer = Circle(drawer.bounds.center, 200.0)
        val inner = Circle(drawer.bounds.center, 150.0)
        val annulus = Shape(listOf(outer.contour.clockwise, inner.contour.counterClockwise))
        val rectangle = Rectangle.fromCenter(drawer.bounds.center, 200.0, 400.0)
        val line = LineSegment(100.0, 400.0, 700.0, 400.0)
        val circle2 = Circle(200.0, 300.0, 100.0)
        val hobbyPts = listOf(
            Vector2(750.0, 100.0),
            Vector2(700.0, 300.0),
            Vector2(600.0, 350.0),
            Vector2(450.0, 450.0),
            Vector2(475.0, 200.0),
        )
        val hobby = hobbyCurve(hobbyPts, closed=true)
        val lineBelow = hobbyCurve(listOf(
            Vector2(100.0, 700.0),
            Vector2(300.0, 725.0),
            Vector2(500.0, 675.0),
            Vector2(700.0, 700.0),
        ))
        val circleAbove = Circle(100.0, 100.0, 50.0)

        // Construct an arrangement
        val arrangement = Arrangement(annulus, rectangle, circle2, hobby, line, lineBelow, circleAbove)

        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)

                // Draw the faces that originate from (are a subset of) some input shape
                val faces = arrangement.originFaces
                for ((i, f) in faces.shuffled(Random(0)).withIndex()) {
                    stroke = null
                    fill = ColorOKHSVa(i * 360.0 / faces.size, 0.75, 1.0).toRGBa()
                    contour(f.contour)
                }

                // Draw the edges
                for (e in arrangement.edges) {
                    strokeWeight = 2.0
                    stroke = ColorRGBa.BLACK
                    fill = null
                    contour(e.contour)
                }

                // Thicken the outer boundaries of each connected component and the 'holes' of the arrangement.
                // Holes are faces that are not a subset of an input shape.
                strokeWeight = 4.0
                contours(arrangement.boundaries)
                contours(arrangement.holes.map { it.contour })

                // Draw the vertices
                for (v in arrangement.vertices) {
                    strokeWeight = 2.5
                    stroke = ColorRGBa.BLACK
                    fill = ColorRGBa.WHITE
                    circle(v.pos, 6.0)
                }
            }
        }
    }
}