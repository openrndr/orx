import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.OptionParameter

/**
 * A simple demonstration of a GUI with a drop-down menu.
 *
 * The entries in the drop-down menu are taken from an `enum class`.
 * The `enum class` entries contain both a name (used in the drop-down)
 * and a `ColorRGBa` instance (used for rendering).
 */

enum class BackgroundColors2(val color: ColorRGBa) {
    Pink(ColorRGBa.PINK),
    Black(ColorRGBa.BLACK),
    Yellow(ColorRGBa.YELLOW)
}

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        val settings = @Description("Settings") object {
            @OptionParameter("Background color")
            var option = BackgroundColors2.Pink
        }

        gui.add(settings)
        extend(gui)
        gui.onChange { name, value ->
            println("$name: $value")
        }
        extend {
            drawer.clear(settings.option.color)
        }
    }
}