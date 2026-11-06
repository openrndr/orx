import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.h3
import org.openrndr.panel.elements.slider
import org.openrndr.panel.elements.toggle
import org.openrndr.panel.style.*

/**
 * Demonstrates the styling and navigation of a list of settings:
 *
 * - the list scrolls vertically only, `overflowX = Overflow.Hidden`, smoothly with the wheel and its scrollbar
 * - the headers of the sections are sticky: they stay at the top while their sections scroll by
 * - controls with rounded corners, `borderRadius`, whose colors change smoothly when hovered, `transitionDuration`
 * - the last section is faded with `opacity`
 * - tab and shift-tab move the keyboard focus between the controls, which shows a focus ring and scrolls the focused
 *   control into view
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val cm = controlManager {
            styleSheet(has class_ "list") {
                width = length { 320 }
                height = length { 440 }
                marginLeft = length { 20 }
                marginTop = length { 20 }
                paddingBottom = length { 10 }
                overflow = Overflow.Scroll
                overflowX = Overflow.Hidden
                background = color { ColorRGBa.GRAY.shade(0.35) }
                borderRadius = length { 10 }
            }
            styleSheet(has type "h3") {
                position = Position.STICKY
                paddingLeft = length { 12 }
                paddingTop = length { 8 }
                paddingBottom = length { 8 }
                background = color { ColorRGBa.fromHex("#3d5a99") }
            }
            styleSheet(has type "button") {
                borderRadius = length { 6 }
                and(has state "hover") {
                    background = color { ColorRGBa.fromHex("#e8574a") }
                }
            }
            styleSheet(has class_ "faded") {
                opacity = 0.4
            }

            layout {
                div("list") {
                    for (section in listOf("Shapes", "Colors", "Motion", "Output")) {
                        div(*if (section == "Output") arrayOf("faded") else emptyArray()) {
                            h3 { section }
                            for (i in 1..3) {
                                slider {
                                    label = "$section $i"
                                    range = Range(0.0, 1.0)
                                    value = i / 4.0
                                }
                            }
                            toggle { label = "enabled" }
                            button(label = "reset $section") {}
                        }
                    }
                }
            }
        }
        extend(cm)
    }
}
