import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.timeoperators.LFOWave
import org.openrndr.extra.timeoperators.TimeOperators

fun main() {
    application {
        program {
            val size = LFO()
            val rotation = LFO(LFOWave.Sine)
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.delayFrames = 10
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend(TimeOperators()) {
                track(size, rotation)
            }
            extend {
                drawer.isolated {
                    drawer.stroke = ColorRGBa.PINK
                    drawer.fill = ColorRGBa.PINK

                    drawer.translate(width / 2.0, height / 2.0)
                    drawer.rotate(rotation.sample() * 180.0)

                    val side = (size.sample(0.5) * 400.0) / 2.0
                    val offset = side / 2.0

                    drawer.rectangle(0.0 - offset, 0.0 - offset, side, side)
                }
            }
        }
    }
}