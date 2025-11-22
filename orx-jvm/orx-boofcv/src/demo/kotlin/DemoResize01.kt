import org.openrndr.application
import org.openrndr.boofcv.binding.resizeBy
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2

/**
 * Demonstrates how to scale down images using the `resizeBy` BoofCV-based
 * method.
 */
fun main() = application {
    program {
        val input = loadImage("demo-data/images/image-001.png")

        val scaled = input.resizeBy(0.5)
        val scaled2 = input.resizeBy(0.25, convertToGray = true)
        val scaled3 = input.resizeBy(0.1)

        println("${input.width} x ${input.height}")
        println("${scaled.width} x ${scaled.height}")

        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.translate(0.0, (height - scaled.bounds.height) / 2.0)

            // Display the loaded image to the right of `scaled` matching its size
            drawer.image(input, scaled.bounds.movedBy(Vector2.UNIT_X * scaled.bounds.width))

            // Display actually scaled down versions of the loaded image
            drawer.image(scaled)
            drawer.image(scaled2, scaled.bounds.width, scaled.bounds.height - scaled2.height)
            drawer.image(scaled3, scaled.bounds.width + scaled2.bounds.width, scaled.bounds.height - scaled3.height)
        }
    }
}
