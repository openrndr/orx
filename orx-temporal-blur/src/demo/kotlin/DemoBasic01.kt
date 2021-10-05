import org.openrndr.applicationSynchronous
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.math.Polar

fun main() = applicationSynchronous {
    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend(TemporalBlur()) {
            samples = 10
            duration = 0.9
        }

        extend {
            drawer.circle(Polar(seconds * 360.0, 200.0).cartesian + drawer.bounds.center, 50.0)
        }
    }
}