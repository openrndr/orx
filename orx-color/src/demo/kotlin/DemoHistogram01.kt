// Show color histogram of an image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.color.statistics.calculateHistogramRGB

fun main() = application {
    program {
        val useColors = 32
        val image = loadImage("demo-data/images/image-001.png")

        val histogram = calculateHistogramRGB(image, binCount = 8)
        print("Histogram using ${histogram.binCount} bins per RGB channel")

        val colorsSortedByFreq = histogram.sortedColors()
        println(" therefore it contains ${colorsSortedByFreq.size} colors.")

        val topColors = colorsSortedByFreq.subList(0, useColors)
        print("\nWe will use the most common $useColors")

        val topColorsSortedByLuminosity = topColors.sortedBy {
            it.first.toHSLa().l
        }
        println(" and sort them by luminosity.")

        val topColorsFreqSum = topColors.sumOf { it.second }
        println("\nThose top $useColors colors represent " +
                String.format("%.02f", 100 * topColorsFreqSum) +
                "% of the image colors.")

        extend {
            drawer.image(image)
            drawer.stroke = null
            var x = 0.0
            topColorsSortedByLuminosity.forEachIndexed { i, (color, freq) ->
                drawer.fill = color

                // draw horizontal bars
                drawer.rectangle(x, 2.0, width.toDouble(), 16.0)
                x += width * freq / topColorsFreqSum

                // draw vertical bars
                drawer.rectangle(i * width / useColors.toDouble() + 2.0,
                        height - 2.0,
                        width / useColors.toDouble() - 4.0,
                        -height * freq / topColorsFreqSum)
            }
        }
    }
}
