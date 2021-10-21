
import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gitarchiver.GitArchiver

fun main() = application {
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