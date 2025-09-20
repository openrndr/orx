import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.colorpickerButton

/**
 * A simple demonstration of a ColorPickerButton
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        var bgColor = ColorRGBa.PINK

        val cm = controlManager {
            layout {
                colorpickerButton {
                    label = "Pick color"
                    color = bgColor
                    events.valueChanged.listen {
                        bgColor = it.color
                    }
                }
            }
        }
        extend(cm)
        extend {
            drawer.clear(bgColor)
        }
    }
}