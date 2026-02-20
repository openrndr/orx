import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.panel.ControlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.bind
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.requestRedraw
import org.openrndr.panel.elements.slider
import org.openrndr.panel.elements.style
import org.openrndr.panel.elements.viewBox
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.FlexDirection
import org.openrndr.panel.style.background
import org.openrndr.panel.style.color
import org.openrndr.panel.style.defaultStyles
import org.openrndr.panel.style.display
import org.openrndr.panel.style.flexDirection
import org.openrndr.panel.style.gridTemplate
import org.openrndr.panel.style.gridTemplateColumns
import org.openrndr.panel.style.gridTemplateRows
import org.openrndr.panel.style.height
import org.openrndr.panel.style.length
import org.openrndr.panel.style.percent
import org.openrndr.panel.style.width
import org.openrndr.shape.Rectangle

/**
 * This demonstration shows how a previously defined `ViewBox` (from `orx-view-box`) can be embedded as a document element within the panel layout system,
 * allowing OPENRNDR drawing operations to be integrated alongside other UI components in a grid layout.
 * The ViewBox element responds interactively to UI controls (slider) and mouse input.
 */


fun main() {
    application {
        configure {
            windowResizable = true
        }
        program {
            class Model {
                var radius = 100.0
            }
            val model = Model()

            val vb = viewBox(Rectangle(0.0, 0.0, 100.0, 100.0)) {
                extend(Camera2D())
                extend {
                    drawer.clear(ColorRGBa.PINK)
                    drawer.circle(mouse.position, model.radius)
                    drawer.circle(drawer.bounds.center, model.radius)

                }
            }
            val cm = ControlManager()
            cm.document = document {
                styleSheets.addAll(defaultStyles())
                body {
                    div {
                        style {
                            width = length { 100.percent }
                            height = length { 100.percent }
                            display = Display.GRID
                            gridTemplateColumns = gridTemplate { listOf(length { 200 }, length { 1.fr }) }
                            gridTemplateRows = gridTemplate { listOf(length { 1.fr }) }
                        }
                        div {
                            style {
                                background = color { ColorRGBa.GRAY }
                                display = Display.FLEX
                                flexDirection = FlexDirection.Column
                            }
                            slider {
                                label = "Radius"
                                range = Range(0.0, 400.0)
                                bind(model::radius)
                            }
                        }
                        viewBox(vb)
                    }
                }
            }

            extend(cm)
            extend {
                cm.body?.requestRedraw()

            }
        }
    }
}