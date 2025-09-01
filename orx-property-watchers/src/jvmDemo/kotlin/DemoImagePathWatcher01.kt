import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawImage
import org.openrndr.extra.color.colormatrix.grayscale
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.propertywatchers.watchingImagePath
import org.openrndr.extra.propertywatchers.watchingProperty

fun main() = application {
    program {
        val state = object {
            var path = "demo-data/images/image-001.png"
            val image by watchingImagePath(::path) {
                drawImage(it.width, it.height) {
                    drawer.drawStyle.colorMatrix = grayscale()
                    drawer.image(it)
                }
            }
            val redImage by watchingProperty(::image, cleaner = { it.destroy() }) {
                drawImage(it.width, it.height) {
                    drawer.drawStyle.colorMatrix = tint(ColorRGBa.RED)
                    drawer.image(it)
                }
            }
        }

        extend {
            drawer.image(state.redImage)
        }
    }
}
