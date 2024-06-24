import org.openrndr.KeyModifier
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter

/**
 * Shows how to store and retrieve in-memory gui presets.
 * Keyboard controls:
 * [Left Shift] + [0]..[9] => store current gui values to a preset
 *                [0]..[9] => recall a preset
 */
fun main() = application {
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false

        val presets = MutableList(10) {
            gui.toObject()
        }

        val settings = @Description("Settings") object {
            @IntParameter("a", 1, 10)
            var a = 7

            @IntParameter("b", 1, 10)
            var b = 3

            @ColorParameter("foreground")
            var foreground = ColorRGBa.fromHex("654062")

            @ColorParameter("background")
            var background = ColorRGBa.fromHex("ff9c71")
        }
        gui.add(settings)
        extend(gui)
        extend {
            drawer.clear(settings.background)
            drawer.stroke = settings.background
            drawer.fill = settings.foreground
            // Draw a pattern based on modulo
            for (i in 0 until 100) {
                if (i % settings.a == 0 || i % settings.b == 0) {
                    val x = (i % 10) * 64.0
                    val y = (i / 10) * 48.0
                    drawer.rectangle(x, y, 64.0, 48.0)
                }
            }
        }
        keyboard.keyDown.listen {
            when (it.name) {
                in "0".."9" -> {
                    if (KeyModifier.SHIFT in it.modifiers) {
                        // 1. Get the current gui state, store it in a list
                        presets[it.name.toInt()] = gui.toObject()
                    } else {
                        // 2. Set the gui state
                        gui.fromObject(presets[it.name.toInt()])
                    }
                }
            }
        }
    }
}