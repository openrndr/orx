import org.openrndr.application
import org.openrndr.boofcv.binding.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extensions.SingleScreenshot


fun main() {
    application {
        program {
            // Load an image, convert to BoofCV format using orx-boofcv
            val input = loadImage("demo-data/images/image-001.png")

            val scaled = input.resizeBy(0.5)
            val scaled2 = input.resizeBy(0.25, convertToGray = true)
            val scaled3 = input.resizeBy(0.1)

            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.translate(0.0, (height - scaled.bounds.height) / 2.0)
                drawer.image(scaled)
                drawer.image(scaled2, scaled.bounds.width, scaled.bounds.height - scaled2.height)
                drawer.image(scaled3, scaled.bounds.width + scaled2.bounds.width, scaled.bounds.height - scaled3.height)
            }
        }
    }
}