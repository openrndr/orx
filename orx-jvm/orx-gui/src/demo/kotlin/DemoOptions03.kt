import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.loadImage
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.OptionParameter

/**
 * A simple demonstration of a GUI with a drop-down menu.
 *
 * The entries in the drop-down menu are taken from the `BlendMode` enum class.
 * The selected blend mode is used to render a circle on top of an image.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        val settings = @Description("Settings") object {
            @OptionParameter("Blend Mode")
            var blendMode = BlendMode.DIFFERENCE
        }

        gui.add(settings)
        extend(gui)
        gui.onChange { name, value ->
            println("$name: $value")
        }
        val img = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.imageFit(img, drawer.bounds, fitMethod = FitMethod.Cover)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.YELLOW
            drawer.strokeWeight = 15.0
            drawer.drawStyle.blendMode = settings.blendMode
            drawer.circle(drawer.bounds.center, 150.0)
        }
    }
}