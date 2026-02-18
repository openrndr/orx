import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.Button
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.Slider
import org.openrndr.panel.elements.bind
import org.openrndr.panel.elements.button
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
                        range = Range(0.0, 100.0)
                        id = "slider-1"
                        label = "Slider 1"
                    }
                    slider {
                        range = Range(0.0, 100.0)
                        id = "slider-2"
                        label = "Slider 2"
                    }
                    button {
                        id = "reset"
                        label = "Reset"
                    }
                }
            }
        }
        class Model {
            var a = 0.0
            var b = 10.0
        }
        val model = Model()

        cm.body?.elementWithId<Slider>("slider-1")?.bind(model::a)
        cm.body?.elementWithId<Slider>("slider-2")?.bind(model::b)
        cm.body?.elementWithId<Button>("reset")?.events?.clicked?.listen {
            model.a = 0.0
            model.b = 0.0
        }

        extend(cm)
        extend {
            val grid = drawer.bounds.grid(2, 1).flatten()
            drawer.circle(grid[0].center, model.a)
            drawer.circle(grid[1].center, model.b)
        }
    }
}