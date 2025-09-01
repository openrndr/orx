import org.openrndr.application
import org.openrndr.extra.timer.repeat

fun main() = application {
    program {
        repeat(2.0) {
            println("hello there $seconds")
        }
    }
}
