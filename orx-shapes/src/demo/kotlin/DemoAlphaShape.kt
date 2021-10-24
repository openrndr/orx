import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.AlphaShape
import org.openrndr.math.Vector2
import kotlin.random.Random

fun main() = application {
    program {
        val points = List(20) {
            Vector2(
                Random.nextDouble(width*0.25, width*0.75),
                Random.nextDouble(height*0.25, height*0.75)
            )
        }
        val alphaShape = AlphaShape(points)
        val c = alphaShape.create()
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.contour(c)
            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points, 4.0)
        }
    }
}