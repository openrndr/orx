import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*

/**
 * A simple demonstration of a GUI with a drop down menu
 */

enum class BackgroundColors {
    Pink,
    Black,
    Yellow
}

fun main() = application {
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        val settings = @Description("Settings") object {
            @OptionParameter("Background color")
            var option = BackgroundColors.Pink
        }

        gui.add(settings)
        extend(gui)
        extend {
            when (settings.option) {
                BackgroundColors.Pink -> drawer.clear(ColorRGBa.PINK)
                BackgroundColors.Black -> drawer.clear(ColorRGBa.BLACK)
                BackgroundColors.Yellow -> drawer.clear(ColorRGBa.YELLOW)
            }
        }
    }
}