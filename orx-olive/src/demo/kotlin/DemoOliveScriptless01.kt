import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.olive.Olive
import org.openrndr.math.IntVector2

fun main() {
    application {
        configure {
            position = IntVector2(3440 / 2 + 200, 360)
            width = 1280
            height = 720
        }
        program {
            extend(Olive<Program>()) {
                // -- this block is for automation purposes only
                if (System.getProperty("takeScreenshot") == "true") {
                    scriptLoaded.listen {
                        // -- this is a bit of hack, we need to push the screenshot extension in front of the olive one
                        fun <T : Extension> Program.extendHead(extension: T, configure: T.() -> Unit): T {
                            extensions.add(0, extension)
                            extension.configure()
                            extension.setup(this)
                            return extension
                        }
                        extendHead(SingleScreenshot()) {
                            this.outputFile = System.getProperty("screenshotPath")
                        }
                    }
                }
                script = "orx-olive/src/demo/kotlin/DemoOliveScriptless01.kt"
                program {
                    extend {
                        drawer.background(ColorRGBa.PINK)
                        drawer.circle(width /2.0, height / 2.0, 300.0 + Math.cos(seconds)*100.0)

                        drawer.fill = ColorRGBa.RED
                        drawer.circle(mouse.position, 100.0)
                    }
                }
            }
        }
    }
}