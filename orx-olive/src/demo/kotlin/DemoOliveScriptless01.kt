import org.openrndr.Extension
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.olive.oliveProgram
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        oliveProgram {
            extend {
                drawer.clear(ColorRGBa.GRAY)
                drawer.fill = ColorRGBa.WHITE
                for (i in 0 until 100) {
                    drawer.circle(
                            width / 2.0 + cos(seconds + i) * 320.0,
                            i * 7.2,
                            cos(i + seconds * 0.5) * 20.0 + 20.0)
                }
            }
        }
                // -- this is only needed for the automated screenshots
                .olive.scriptLoaded.listen {
                    if (System.getProperty("takeScreenshot") == "true") {
                        // -- this is a bit of hack, we need to push the screenshot extension in front of the olive one
                        fun <T : Extension> extendHead(extension: T, configure: T.() -> Unit): T {
                            program.extensions.add(0, extension)
                            extension.configure()
                            extension.setup(program)
                            return extension
                        }
                        extendHead(SingleScreenshot()) {
                            this.outputFile = System.getProperty("screenshotPath")
                        }
                    }
                }
    }
}