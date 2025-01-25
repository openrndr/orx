package histogram// Create a simple rectangle composition based on colors sampled from image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.statistics.calculateHistogramRGB

fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val histogram = calculateHistogramRGB(image)
        extend {
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