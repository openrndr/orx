import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.patterns.Checkers

suspend fun main() {
    application {
        program {
            val fd = FluidDistort()
            val checkers =  Checkers()

            val image = colorBuffer(width, height)
            val distorted = image.createEquivalent()
            checkers.size = 64.0
            checkers.apply(emptyArray(), image)

            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                fd.blend = mouse.position.x/width
                fd.apply(image, distorted)
                drawer.image(distorted)
            }
        }
    }
}