import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.hobbyCurve
import org.openrndr.math.Vector2

fun main() = application {
    program {
        extend {
            val points = listOf(Vector2(150.0, 350.0), Vector2(325.0, 100.0), Vector2(500.0, 350.0), Vector2(325.0, 250.0))
            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = ColorRGBa.PINK
            drawer.contour(hobbyCurve(points, closed=true))
        }
    }
}