import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.propertywatchers.watchingProperty
import kotlin.math.log

/**
 * This program demonstrates how to use `watchingProperty()` to track changes
 * to variables.
 *
 * The method is used to define and keep two variables up to date:
 * First, it creates a variable `x` that tracks the horizontal position of the mouse.
 * Second, it creates the variable `xx` with the square of `x` and
 * keeps it up to date by watching it change.
 *
 * Note how the variables are defined just once outside the draw loop
 * but are kept up to date nonetheless.
 *
 * The two variables are used to control the position and the thickness of a
 * horizontal black line.
 */
fun main() = application {
    program {
        val state = object {
            val x by watchingProperty(mouse::position) {
                it.x
            }

            val xx by watchingProperty(::x) {
                it * it
            }
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.strokeWeight = log(state.xx, 1.5)
            drawer.lineSegment(20.0, state.x, width - 20.0, state.x)
        }
    }
}
