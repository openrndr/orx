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
                this.height = 100.percent
                this.width = 200.px
                this.display = Display.FLEX
                this.flexDirection = FlexDirection.Column
                this.paddingLeft = 10.px
                this.paddingRight = 10.px
                this.background = Color.RGBa(ColorRGBa.GRAY)
            }
            styleSheet(has type "slider") {
                this.marginTop = 25.px
                this.marginBottom = 25.px
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