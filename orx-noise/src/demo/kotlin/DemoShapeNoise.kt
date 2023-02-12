import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.PINK
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.rgb
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.filters.*
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.sin

/**
 * Use [Shape.uniform] to find random points inside shapes
 */
fun main() = application {
    program {
        val c = Circle(150.0, 150.0, 80.0)
        val cPoints = List(150) {
            c.uniform(7.0)
        }

        val r = Rectangle(250.0, 70.0, 160.0, 160.0)
        val rPoints = List(150) {
            r.uniform(7.0)
        }

        val s = ShapeContour.fromPoints(
            listOf(
                Vector2(550.0, 50.0),
                Vector2(480.0, 450.0),
                Vector2(100.0, 350.0),
                Vector2(380.0, 320.0)
            ), true
        ).shape
        val sPoints = List(500) {
            s.uniform(7.0)
        }

        extend {
            drawer.clear(rgb(0.20, 0.18, 0.16))

            drawer.fill = PINK
            drawer.circle(c)
            drawer.rectangle(r)
            drawer.shape(s)

            drawer.fill = WHITE
            drawer.circles(cPoints, 5.0)
            drawer.circles(rPoints, 5.0)
            drawer.circles(sPoints, 5.0)
        }
    }
}
