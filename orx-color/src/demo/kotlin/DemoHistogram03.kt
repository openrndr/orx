// Create a simple rectangle composition based on colors sampled from image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.color.statistics.calculateHistogramRGB

fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val histogram = calculateHistogramRGB(image)
        extend {
            drawer.image(image)
            for (j in 0 until height step 32) {
                for (i in 0 until width step 32) {
                    drawer.stroke = null
                    drawer.fill = histogram.sample()
                    drawer.rectangle(i * 1.0, j * 1.0, 32.0, 32.0)
                }
            }
        }
    }
}