package primitives

import org.openrndr.application
import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.alignToVertically
import org.openrndr.extra.shapes.primitives.distributeHorizontally
import kotlin.math.cos
import kotlin.random.Random

/**
 * This program demonstrates three Rectangle-related methods:
 * `uniformSub()`, `distributeHorizontally()` and `alignToVertically()`.
 *
 * `uniformSub` is used to create a sub Rectangle of the window bounds. By default, its arguments
 * allow any width and height between 0.0 and 1.0 (full width). In this program we override
 * the minimum and maximum random widths. `uniformSub` takes a `random` parameter, which
 * we change only once per second by rounding `seconds` to an integer. This randomizes
 * the widths every second.
 *
 * At this point in the program we have a List with 7 random sub rectangles, potentially
 * overlapping each other. By calling `.distributeHorizontally()` we displace the
 * rectangles so the horizontal space between them is equal.
 *
 * Finally, we call `alignToVertically()` with a sine wave of time as an argument, to interpolate
 * their vertical position between being top-aligned and bottom-aligned.
 *
 * Try commenting out one or both of the last two function calls to observe the resulting changes.
 */
fun main() {
    application {
        program {
            extend {
                val random = Random(seconds.toInt())
                val rs = (0 until 7).map { drawer.bounds.uniformSub(minWidth = 0.01, maxWidth = 0.1, random = random) }
                    .distributeHorizontally(drawer.bounds)
                    .alignToVertically(drawer.bounds, cos(seconds) * 0.5 + 0.5)

                drawer.rectangles(rs)
            }
        }
    }
}