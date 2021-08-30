import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.timer.timeOut

fun main() = application {
    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        timeOut(2.0) {
            println("hello there $seconds" )
        }
    }
}