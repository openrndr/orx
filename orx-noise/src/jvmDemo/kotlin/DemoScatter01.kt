import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2
import org.openrndr.shape.Ellipse
import kotlin.math.cos
import kotlin.random.Random

fun main() {
    application {
        program {
            extend {
                val shape = Ellipse(Vector2(width/2.0, height/2.0), 200.0, 150.0 + cos(seconds)*125.0).shape
                val points = shape.scatter(20.0,  random = Random(0))
                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = null
                drawer.fill = ColorRGBa.PINK
                drawer.circles(points, 4.0)

                if (seconds.mod(2.0) < 1.0) {
                    drawer.stroke = ColorRGBa.PINK
                    drawer.fill = null
                    drawer.shape(shape)
                }
            }
        }
    }
}