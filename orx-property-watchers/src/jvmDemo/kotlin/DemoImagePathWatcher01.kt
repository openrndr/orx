import org.openrndr.KEY_ENTER
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawImage
import org.openrndr.extra.color.colormatrix.grayscale
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.propertywatchers.watchingImagePath
import org.openrndr.extra.propertywatchers.watchingProperty

/**
 * Demonstrates `watchingImagePath()` and `watchingProperty()`.
 *
 * `watchingImagePath()` detects changes to a String, loads the image the String points to, and returns it as a
 * ColorBuffer. It allows transforming the loaded image, for instance, by making it grayscale, resizing it,
 * or applying a filter.
 *
 * `watchingProperty()` detects changes to the watched variable which can be of any type. The returned type is not
 * fixed and is determined by whatever is returned by its `function` argument. A `cleaner` argument, if present,
 * will be executed before calling `function`, ideal to free resources.
 *
 * Press the `ENTER` key to update the `state.path` variable, which will trigger an update of `state.image`, followed
 * by an update to `state.redImage`.
 */
fun main() = application {
    program {
        val state = object {
            var path = "demo-data/images/image-001.png"
            val image by watchingImagePath(::path) {
                println("path changed, updating image by making it grayscale")
                drawImage(it.width, it.height) {
                    drawer.drawStyle.colorMatrix = grayscale()
                    drawer.image(it)
                }
            }
            val redImage by watchingProperty(::image, cleaner = { it.destroy() }) {
                println("image changed, updating redImage by tinting it red")
                drawImage(it.width, it.height) {
                    drawer.drawStyle.colorMatrix = tint(ColorRGBa.RED)
                    drawer.image(it)
                }
            }
        }

        extend {
            drawer.image(state.redImage)
        }

        keyboard.keyDown.listen {
            if(it.key == KEY_ENTER) {
                state.path = "demo-data/images/peopleCity01.jpg"
            }
        }
    }
}
