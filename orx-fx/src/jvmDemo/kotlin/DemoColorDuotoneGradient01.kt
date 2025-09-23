import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.color.DuotoneGradient

/**
 * The [DuotoneGradient] effect combines the Duotone effect
 * and a linear gradient: two duotone colors are applied on
 * one part of the image, and those colors are interpolated
 * to two other colors, applied in a different part of the image.
 *
 * The `rotation` parameter lets us specify in which direction
 * the interpolation happens (vertical, horizontal, or something else).
 */
fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val filteredImage = image.createEquivalent()
        val duotone = DuotoneGradient()
        duotone.labInterpolation = false

        extend {
            duotone.labInterpolation = true
            duotone.backgroundColor0 = ColorRGBa.BLACK
            duotone.foregroundColor0 = ColorRGBa.RED
            duotone.backgroundColor1 = ColorRGBa.BLUE
            duotone.foregroundColor1 = ColorRGBa.WHITE
            duotone.rotation = seconds * 45.0
            duotone.apply(image, filteredImage)
            drawer.image(filteredImage)
        }
    }
}