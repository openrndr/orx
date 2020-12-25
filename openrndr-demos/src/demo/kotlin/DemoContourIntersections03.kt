import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.intersections
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    program {
        val points = 200
        extend {
            val contour = ShapeContour.fromPoints(
                    List(points) {
                        val a = PI * 2 * it / points
                        val x = (200 + 50 * cos(a * 2)) * sin(a * 3 + sin(a))
                        val y = 150 * cos(a * 2 + seconds * 0.2)
                        Vector2(x, y)
                    }, closed = true
            )
            val ints = intersections(contour, contour)
            drawer.run {
                clear(ColorRGBa.WHITE)
                translate(width * 0.5, height * 0.5)
                fill = null
                stroke = ColorRGBa.BLACK
                contour(contour)
                fill = ColorRGBa.PINK.opacify(0.3)

                circles(ints.map { it.position }, 10.0)
            }
        }
    }
}