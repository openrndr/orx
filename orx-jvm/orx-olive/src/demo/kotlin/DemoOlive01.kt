import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.applicationSynchronous
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.olive.Olive

fun main() = applicationSynchronous {
    configure {
        width = 768
        height = 576
    }
    program {

        extend(Olive<Program>()) {
            script = "orx-olive/src/demo/kotlin/demo-olive-01.kts"
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
        }
    }
}