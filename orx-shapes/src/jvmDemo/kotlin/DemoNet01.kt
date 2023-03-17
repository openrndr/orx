import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.Net
import org.openrndr.shape.Circle
import kotlin.math.sin

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = ColorRGBa.PINK
                val a = drawer.bounds.position(0.7, 0.5)
                val b = drawer.bounds.position(0.3, 0.5)
                val c = Circle(
                    drawer.bounds.position(
                        sin(seconds) * 0.35 + 0.5,
                        sin(seconds * 2) * 0.25 + 0.5
                    ), 50.0
                )
                val net = Net(a, b, c)
                drawer.circle(a, 10.0)
                drawer.circle(b, 10.0)
                drawer.circle(c)

                drawer.strokeWeight = 2.0
                drawer.contour(net.contour)
            }
        }
    }
}