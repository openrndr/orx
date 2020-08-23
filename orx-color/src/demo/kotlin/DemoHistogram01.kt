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

        val histogram = calculateHistogramRGB(image, binCount = 8)
        println("Histogram calculated with ${histogram.binCount} bins per channel")

        val colorsSortedByFreq = histogram.sortedColors()
        println("therefore it contains ${colorsSortedByFreq.size} colors.")

        val topColors = colorsSortedByFreq.subList(0, 32)
        val topColorsSortedByLuminosity = topColors.sortedBy {
            it.first.toHSLa().l
        }
        extend {
            drawer.image(image)
            drawer.stroke = null
            topColorsSortedByLuminosity.forEachIndexed { i, (color, freq) ->
                drawer.fill = color
                drawer.rectangle(i * width / 32.0 + 2.0,
                        height - 2.0,
                        width / 32.0 - 4.0,
                        -1000 * freq)
            }
        }
    }
}
