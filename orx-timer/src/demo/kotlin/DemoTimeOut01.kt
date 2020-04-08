import org.openrndr.application
import org.openrndr.extra.timer.timeOut

fun main() = application {
    program {
        timeOut(2.0) {
            println("hello there $seconds" )
        }
    }
}