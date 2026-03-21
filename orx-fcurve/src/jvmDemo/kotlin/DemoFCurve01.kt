import org.openrndr.application
import org.openrndr.extra.fcurve.fcurve

/**
 * Demonstrates how to create two `FCurve` instances, and a sampler function out of each.
 *
 * The first `FCurve` is used to control the horizontal position of a circle. It starts
 * at the left edge of the window (0.0), reaches the center of the program window (360.0)
 * after 4 seconds, and the right edge of the window (720.0) at 5 seconds.
 *
 * The second `FCurve` controls the vertical position, and it starts at the vertical center
 * of the window (360.0) and holds that position for 5 seconds.
 *
 * By using `seconds.mod(5.0)` the time repeats in a loop that starts at 0.0 and ends at 5.0,
 * producing an animation that repeats every 5 seconds.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val xpos = fcurve("M0 Q4,360,5,720").sampler()
        val ypos = fcurve("M360 h5").sampler()

        extend {
            val t = seconds.mod(5.0)
            drawer.circle(xpos(t), ypos(t), 100.0)
        }
    }
}
