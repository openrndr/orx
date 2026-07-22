import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Shows how to use `loopSampler()` to repeatedly sample an `fcurve` in a loop.
 *
 * Changing `normalized` has no visible effect in this demo because the
 * curve duration is exactly 1.0, so normalized time and seconds are equivalent.
 *
 * The demo visualizes the sampled curve in two ways: as an animated circle
 * and as a static contour.
 */
fun main() = application {
    program {
        val f = fcurve(
            """
            M0
            C0.2,0.0,0.3,1.0,0.5,1.0
            C0.1,1.0,0.0,0.0,0.5,0.0
            """
        )
        println(f.duration) // 1.0
        val lfo = f.loopSampler(normalized = true)

        extend {
            drawer.clear(ColorRGBa.PINK)

            // Demonstrates the looping animation tied the current time in seconds.
            // The loop repeats every second (because the length of the fcurve is 1.0).
            drawer.circle(drawer.bounds.center, lfo(seconds) * 50.0 + 50.0)

            // Demonstrates the looping of the fcurve as a contour, useful
            // to show the shape of the curve on a screenshot
            val points = List(width) { x ->
                // we need to map pixels to time: 125 pixels = 1 second
                Vector2(x.toDouble(), height - 50.0 - lfo(x / 125.0) * 50.0)
            }
            drawer.strokeWeight = 2.0
            drawer.contour(ShapeContour.fromPoints(points, false))
        }
    }
}
