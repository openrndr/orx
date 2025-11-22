import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.PathParameter
import org.openrndr.extra.propertywatchers.watchingImagePath

/**
 * Demonstrates how to include a button for loading images in a GUI, and how to display
 * the loaded image.
 *
 * The program applies the `@PathParameter` annotation to a `String` variable, which gets
 * rendered by the GUI as an image-picker button. Note the allowed file `extensions`.
 *
 * This mechanism only updates the `String` containing the path of an image file.
 *
 * The `watchingImagePath()` delegate property is used to automatically load an image
 * when its `String` argument changes.
 */
fun main() = application {
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false

        val settings = @Description("Settings") object {
            @PathParameter("image", extensions = ["jpg", "png"], order = 10)
            var imagePath = "demo-data/images/image-001.png"

            val image by watchingImagePath(::imagePath) {
                it
            }
        }
        gui.add(settings)
        extend(gui)
        extend {
            drawer.image(settings.image)
        }
    }
}
