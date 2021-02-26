import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot

fun main() {
    application {
        configure {
            width = 800
            height = 800
            title = "QuadTree"
        }
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)


            }
        }
    }
}