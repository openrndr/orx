import org.openrndr.extra.fx.color.RgbToOkLab
import org.openrndr.extra.fx.color.OkLabToRgb
import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage

/**
 * This demonstrates converting a [ColorBuffer] from and to (OK)LAB color space using the [RgbToOkLab] and [OkLabToRgb]
 * filters. The (OK)Lab representation is signed and requires a floating point representation.
 */

fun main() {
    application {
        program {
            val rgbToOkLab = RgbToOkLab()
            val okLabToRgb = OkLabToRgb()
            val image = loadImage("demo-data/images/image-001.png")
            val labImage = image.createEquivalent(type = ColorType.FLOAT32)
            rgbToOkLab.apply(image, labImage)
            okLabToRgb.apply(labImage, image)
            extend {
                drawer.image(image)
            }
        }
    }
}