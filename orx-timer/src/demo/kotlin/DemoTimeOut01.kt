import org.openrndr.application
import org.openrndr.extra.timer.timeOut

/**
 * Demonstrates the `timeOut` function.
 *
 * It is similar to the `repeat` function,
 * but it runs only once after the specified delay in seconds.
 *
 */
fun main() = application {
    program {
        timeOut(2.0) {
            println("hello there $seconds")
        }
    }
}
