import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2

/**
 * A simple demonstration of a GUI for drawing a single circle
 */

enum class SomeOptions {
    Default,
    DoNothing,
    Smile
}

fun main() = application {
    program {
        val gui = GUI()
        val settings = @Description("Settings") object {
            @OptionParameter("action")
            var option = SomeOptions.Default
        }

        gui.add(settings)
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend(gui)
        extend {
            when(settings.option) {
                SomeOptions.Default -> drawer.background(ColorRGBa.PINK)
                SomeOptions.DoNothing -> drawer.background(ColorRGBa.BLACK)
                SomeOptions.Smile -> drawer.background(ColorRGBa.YELLOW)
            }
        }
    }
}