import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.color.DuotoneGradient

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