import org.openrndr.application
import org.openrndr.boofcv.binding.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage


fun main() {
    application {
        program {
            // Load an image, convert to BoofCV format using orx-boofcv
            val input = loadImage("demo-data/images/image-001.png").toPlanarU8()

            val scaled = input.resize(0.5).toColorBuffer()
            val scaled2 = input.resize(0.25).toColorBuffer()
            val scaled3 = input.resize(0.1).toColorBuffer()

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