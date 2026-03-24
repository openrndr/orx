import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.delegatemagic.tracking.tracking
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2

/**
 * Demonstrates the use of the `tracking` delegate.
 *
 * In this case it is used to visualize the history of a variable
 * which changes based on the simplex noise of the current time in seconds.
 */
fun main() = application {
    program {
        val state = object {
            var radius = 100.0
            val radiusHistory by tracking(::radius, width)
        }
        if (System.getProperty("takeScreenshot") == "true") {
            extensions.filterIsInstance<SingleScreenshot>().forEach {
                // Delay the automatic screenshot so `radiusHistory` is not empty
                it.delayFrames = 180
            }
        }
        extend {
            // Update `radius`, which updates `radiusHistory`
            state.radius = simplex(8086, seconds) * 100.0 + 100.0
            drawer.circle(drawer.bounds.center, state.radius)

            // Map the Double values in radiusHistory to Vector2
            val points = state.radiusHistory.mapIndexed { x, y ->
                Vector2(x * 1.0, height - y)
            }
            drawer.strokeWeight = 2.0
            drawer.stroke = ColorRGBa.RED
            drawer.lineStrip(points)
        }
    }
}
