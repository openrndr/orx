import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.timer.repeat

/**
 * Shows how a `repeat` block can update a variable used
 * for rendering. In this demo, the `opacity` variable is
 * reduced on every animation frame, and increased to 1.0
 * every 2 seconds, creating a pulsating animation effect.
 */
fun main() = application {
    program {
        var opacity = 0.0
        repeat(2.0) {
            opacity = 1.0
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.BLACK.opacify(opacity)
            drawer.fill = ColorRGBa.WHITE.opacify(opacity)
            drawer.circle(width / 2.0, height / 2.0, 200.0)
            opacity *= 0.9
        }
    }
}
