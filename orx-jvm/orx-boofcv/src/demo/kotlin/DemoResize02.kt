import org.openrndr.application
import org.openrndr.boofcv.binding.resizeTo
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage

/**
 * Demonstrates how to scale down images using the `resizeTo` BoofCV-based
 * method.
 *
 * If only the `newWidth` or the `newHeight` arguments are specified,
 * the resizing happens maintaining the original aspect ratio.
 */
fun main() = application {
    program {
        val input = loadImage("demo-data/images/image-001.png")

        val scaled = input.resizeTo(input.width / 3)
        val scaled2 = input.resizeTo(newHeight = input.height / 4, convertToGray = true)
        val scaled3 = input.resizeTo(input.width / 5, input.height / 5)

        println("${input.width} x ${input.height}")
        println("${scaled.width} x ${scaled.height}")

        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.translate(0.0, (height - scaled.bounds.height) / 2.0)

            // Display actually scaled down versions of the loaded image
            drawer.image(scaled)
            drawer.image(scaled2, scaled.bounds.width, scaled.bounds.height - scaled2.height)
            drawer.image(scaled3, scaled.bounds.width + scaled2.bounds.width, scaled.bounds.height - scaled3.height)
        }
    }
}
