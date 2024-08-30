import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.timeoperators.Envelope
import org.openrndr.extra.timeoperators.TimeOperators

fun main() {
    application {
        program {
            val size = Envelope(50.0, 400.0, 0.5, 0.5)
            val rotation = Envelope(easingFactor = 0.4)

            extend(TimeOperators()) {
                track(size, rotation)
            }
            extend {
                if (frameCount % 80 == 0) {
                    size.trigger()
                }

                if (frameCount % 50 == 0) {
                    rotation.trigger(180.0)
                }

                drawer.isolated {
                    drawer.stroke = ColorRGBa.PINK
                    drawer.fill = ColorRGBa.PINK

                    drawer.translate(width / 2.0, height / 2.0)
                    drawer.rotate(rotation.value)

                    val side = size.value / 2.0
                    val offset = side / 2.0

                    drawer.rectangle(0.0 - offset, 0.0 - offset, side, side)
                }
            }
        }
    }
}