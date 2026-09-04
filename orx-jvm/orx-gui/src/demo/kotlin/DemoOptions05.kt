import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.MutableListParameter
import org.openrndr.panel.collections.SelectableMutableList
import java.io.File

/**
 * Demonstrates how to populate a [MutableListParameter] in a GUI
 * with the files found in a folder.
 * The image is loaded and displayed when the user chooses
 * an image from the drop-down menu.
 * The program allows dropping a folder onto the program populate
 * the drop-down menu.
 */

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val gui = GUI()
        var img: ColorBuffer? = null

        // A map from names (for displaying) to absolute paths (for loading)
        val imageMap = mutableMapOf<String, String>()
        File("demo-data/images").listFiles()?.forEach {
            if (it.extension.lowercase() in listOf("png", "jpg"))
                imageMap[it.name] = it.absolutePath
        }

        val settings = @Description("Settings") object {
            @MutableListParameter("Images")
            var images = SelectableMutableList(imageMap.keys)
        }

        gui.compartmentsCollapsedByDefault = false
        gui.persistState = false
        gui.add(settings)
        gui.onChange { name, value ->
            when (name) {
                "images" -> {
                    // Load the image selected in the drop-down
                    img?.destroy()
                    img = imageMap[value]?.let { loadImage(it) }
                }
            }
        }
        window.drop.listen { ev ->
            val f = ev.files.first()
            if (File(f).isDirectory) {
                // repopulate the map
                imageMap.clear()
                File(f).listFiles()?.forEach {
                    if (it.extension.lowercase() in listOf("png", "jpg"))
                        imageMap[it.name] = it.absolutePath
                }
                // repopulate the mutable list parameter
                settings.images.clear()
                settings.images.addAll(imageMap.keys)
            }
        }
        extend(gui)
        extend {
            img?.let {
                drawer.imageFit(it, drawer.bounds, fitMethod = FitMethod.Cover)
            }
        }
    }
}