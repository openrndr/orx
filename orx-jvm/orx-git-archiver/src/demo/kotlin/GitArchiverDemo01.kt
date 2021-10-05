import org.openrndr.applicationSynchronous
import org.openrndr.extensions.Screenshots
import org.openrndr.extensions.SingleScreenshot

fun main() = applicationSynchronous {
    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        val ga = extend(GitArchiver()) {
            commitOnRun = true
            commitOnRequestAssets = false
        }
        extend(Screenshots())
        extend {


        }
    }

}