import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

/**
 * Demonstrates tabs, number fields, tooltips and context menus:
 *
 * - the tabs at the top switch between pages
 * - number fields change by dragging them left and right, more finely with shift, or by typing: double-click a field or
 *   press enter. The arrow keys change the focused field
 * - resting the pointer on a field shows its tooltip
 * - clicking a number field with the right button opens its context menu
 * - the text field selects by dragging, copies, cuts and pastes with the keyboard
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        var radius = 80.0
        var hue = 20.0
        lateinit var radiusField: NumberField

        val cm = controlManager {
            styleSheet(has class_ "panel") {
                width = 280.px
                height = 100.percent
                padding(10.px)
                background = color { ColorRGBa.GRAY.shade(0.3) }
            }

            layout {
                div("panel") {
                    tabs {
                        tab("Numbers") {
                            radiusField = numberField {
                                label = "radius"
                                range = 0.0..200.0
                                step = 0.5
                                precision = 1
                                value = radius
                                tooltip = "drag to change the radius,\nhold shift for finer steps"
                                events.valueChanged.listen { radius = it.newValue }
                                contextMenu {
                                    item("reset") { value = 80.0 }
                                    item("smallest") { value = 0.0 }
                                    item("largest") { value = 200.0 }
                                }
                            }
                            numberField {
                                label = "hue"
                                step = 1.0
                                precision = 0
                                value = hue
                                tooltip = "the hue of the circle, in degrees"
                                events.valueChanged.listen { hue = it.newValue }
                            }
                        }
                        tab("Text") {
                            textfield {
                                label = "name"
                                value = "a circle"
                            }
                        }
                        tab("About") {
                            p { "a demo of tabs, number fields, tooltips and context menus" }
                        }
                    }
                }
            }
        }
        extend(cm)
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.fill = org.openrndr.color.ColorHSVa(hue, 0.6, 0.9).toRGBa()
            drawer.stroke = null
            drawer.circle(500.0, 240.0, radius)
        }
    }
}
