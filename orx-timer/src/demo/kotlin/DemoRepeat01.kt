import org.openrndr.application
import org.openrndr.extra.timer.repeat

/**
 * A simple demonstration on using the `repeat` method to execute a function
 * at regular intervals.
 *
 * Note that drawing inside the repeat action has no effect.
 * See DemoRepeat02.kt to learn how to trigger drawing.
 *
 */
fun main() = application {
    program {
        repeat(2.0) {
            println("hello there $seconds")
        }
    }
}
