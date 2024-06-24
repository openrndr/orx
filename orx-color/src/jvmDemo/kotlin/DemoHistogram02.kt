// Show color histogram using non-uniform weighting

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.statistics.calculateHistogramRGB
import kotlin.math.pow

fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        // -- here we use non-uniform weighting, such that bright colors are prioritized
        val histogram = calculateHistogramRGB(image, weighting = {
            ((r + g + b) / 3.0).pow(2.4)
        })
        val colors = histogram.sortedColors()
        // .subList(0, 32).sortedBy { it.first.toHSLa().h } // sort by hue

        extend {
            drawer.image(image)
            for (i in 0 until 32) {
                drawer.fill = colors[i].first
                drawer.stroke = null
                drawer.rectangle(i * width / 32.0 + 2.0,
                        height - 2.0,
                        width / 32.0 - 4.0,
                        -16.0)
            }
        }
    }
}