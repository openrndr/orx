import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.PathParameter
import org.openrndr.extra.propertywatchers.watchingImagePath

fun main() {
    application {
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
}