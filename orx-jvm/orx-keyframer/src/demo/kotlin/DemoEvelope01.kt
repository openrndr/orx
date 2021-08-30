import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

fun main() = application {
    program {
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-envelope-01.json")))
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {
            animation(seconds)
            drawer.circle(animation.position, 100.0)
        }
    }
}