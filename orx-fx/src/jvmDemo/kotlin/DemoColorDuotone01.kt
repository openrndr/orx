import org.openrndr.extra.fx.color.Duotone
import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.math.mod_

fun main() {
    application {
        program {

            val image = loadImage("demo-data/images/image-001.png")
            val filteredImage = image.createEquivalent()
            val duotone = Duotone()

            extend {

                duotone.labInterpolation = seconds.mod_(2.0) < 1.0
                duotone.apply(image, filteredImage)
                drawer.image(filteredImage)

            }

        }

    }
}