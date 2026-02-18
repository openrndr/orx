import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.slider
import org.openrndr.panel.style.*

fun main() = application {
    program {
        val cm = controlManager {
            styleSheet(has class_ "side-bar") {
                this.height = length { 100.percent }
                this.width = length { 200 }
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Column
                this.paddingLeft = length { 10 }
                this.paddingRight = length { 10 }
                this.background = color { ColorRGBa.GRAY }
            }
            styleSheet(has type "slider") {
                this.marginTop = length { 25 }
                this.marginBottom = length { 25 }
            }
            layout {
                div("side-bar") {
                    slider {
                        label = "Slider 1"
                    }
                    slider {
                        label = "Slider 2"
                    }
                }
            }
        }
        extend(cm)
        extend {

        }
    }
}