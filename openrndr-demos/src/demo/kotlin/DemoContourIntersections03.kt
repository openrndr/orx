import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.intersections
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    program {
        val contour = ShapeContour.fromPoints(
                List(80) {
                    val a = PI * 2 * it / 80.0
                    val x = 200.0 * sin(a * 2)
                    val y = 200.0 * cos(a)
                    Vector2(x, y)
                }, closed = true
        )
        val ints = intersections(contour, contour)
        extend {
            drawer.run {
                clear(ColorRGBa.WHITE)
                translate(width * 0.5, height * 0.5)
                fill = null
                stroke = ColorRGBa.BLACK
                contour(contour)
                circles(ints.map { it.position }, 10.0)
            }
        }
    }
}