import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.launch
import org.openrndr.panel.ControlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

/**
 * This demonstration shows how `ViewBox` (from `orx-view-box`) can be embedded as a document element within the panel layout system,
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
                        viewBox(this@program) {
                            extend(Camera2D())
                            extend {
                                drawer.clear(ColorRGBa.PINK)
                                drawer.circle(mouse.position, model.radius)
                                drawer.circle(drawer.bounds.center, model.radius)
                                program.launch {
                                    this@body.requestRedraw()
                                }
                            }
                        }
                    }
                }
            }
            window.sized.listen {
                cm.body?.requestRedraw()
            }

            extend(cm)
            extend {

            }
        }
    }
}