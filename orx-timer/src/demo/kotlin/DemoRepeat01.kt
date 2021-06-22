import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.timer.repeat

suspend fun main() = application {
    program {
        repeat(2.0) {
            println("hello there $seconds" )
        }
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {

        }
    }
}