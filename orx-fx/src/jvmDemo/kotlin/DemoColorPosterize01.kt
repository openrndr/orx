import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.color.Posterize

/**
 * Demonstration of the [Posterize] effect to reduce the number of colors
 * present in an image.
 */
fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val filteredImage = image.createEquivalent()
        val posterize = Posterize()

        extend {
            posterize.levels = 2
            posterize.apply(image, filteredImage)
            drawer.image(filteredImage)
        }
    }
}
