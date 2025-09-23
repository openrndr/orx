import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.color.Duotone

/**
 * This demo shows how to use the [Duotone] filter,
 * toggling the `labInterpolation` parameter every second on and off.
 *
 * The `foregroundColor` and `backgroundColor` parameters are
 * left to their defaults.
 */
fun main() = application {
    program {

        val image = loadImage("demo-data/images/image-001.png")
        val filteredImage = image.createEquivalent()
        val duotone = Duotone()

        extend {
            duotone.labInterpolation = seconds.mod(2.0) < 1.0
            duotone.apply(image, filteredImage)
            drawer.image(filteredImage)
        }
    }
}
