import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.timeoperators.Envelope
import org.openrndr.extra.timeoperators.TimeOperators
import org.openrndr.extra.timer.repeat
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Demonstrates the use of an Attack/Decay `Envelope` to control the size and rotation of a rectangle.
 *
 * The size envelope is triggered every 1.3 seconds, and the rotation one every 0.8 seconds.
 */
fun main() = application {
    program {
        val size = Envelope(50.0, 400.0, 0.5, 0.5)
        val rotation = Envelope(easingFactor = 0.4)
        extend(TimeOperators()) {
            track(size, rotation)
        }
        repeat(1.3) {
            size.trigger()
        }
        repeat(0.8) {
            rotation.trigger(180.0)
        }
        extend {
            drawer.isolated {
                drawer.stroke = null
                drawer.fill = ColorRGBa.PINK

                drawer.translate(width / 2.0, height / 2.0)
                drawer.rotate(rotation.value)

                drawer.rectangle(Rectangle.fromCenter(Vector2.ZERO, size.value / 2.0))
            }
        }
    }
}
