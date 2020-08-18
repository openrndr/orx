// Show color histogram of an image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.statistics.calculateHistogramRGB

fun main() = application {
    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        val image = loadImage("demo-data/images/image-001.png")
        val histogram = calculateHistogramRGB(image)
        val colors = histogram.sortedColors()
        extend {

            drawer.image(image)
            for (i in 0 until 32) {
                drawer.fill = colors[i].first
                drawer.stroke = null
                drawer.rectangle(i * (width/32.0), height-16.0, width/32.0, 16.0)
            }

        }
    }
}