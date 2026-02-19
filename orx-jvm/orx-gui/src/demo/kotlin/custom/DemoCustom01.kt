import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.extra.gui.custom.uiForParameters
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import java.io.File

/**
 * Demonstrates the use of grid layouts, property-control binding, and JSON serialization/deserialization for
 * model persistence.
 */

// <editor-fold desc="Model definition">
class Model {
    @ActionParameter("Perform some action")
    fun action() {
        color = ColorRGBa(Double.uniform(0.0, 1.0), Double.uniform(0.0, 1.0), Double.uniform(0.0, 1.0), 1.0)
    }

    @DoubleParameter("radius", 0.0, 100.0, order = 0)
    var radius = 100.0

    @ColorParameter("color", order = 1)
    var color = ColorRGBa.PINK

    @BooleanParameter("enable stroke", order = 2)
    var enableStroke = false

    @IntParameter("circle count", 1, 10, order = 3)
    var circleCount = 1

    @TextParameter("text", order = 4)
    var text = ""

    @Vector2Parameter("position", 0.0, 1.0, order = 5)
    var position = Vector2(0.5, 0.5)

    fun copyFrom(other: Model) {
        this.radius = other.radius
        this.color = other.color
        this.enableStroke = other.enableStroke
        this.circleCount = other.circleCount
        this.text = other.text
        this.position = other.position
    }

    companion object {
        fun loadFromJson(file: File): Model {
            return Json.decodeFromString<Model>(file.readText())
        }

        fun saveToJson(file: File, model: Model) {
            file.writeText(Json.encodeToString(model))
        }
    }
}
// </editor-fold>

fun main() {
    application {
        configure {
            width = 720
            height = 720
            windowResizable = true
        }
        program {
            val model = Model()
            val cm = controlManager {

                // <editor-fold desc="UI styles">
                styleSheet(has class_ "cell") {
                    width = length { auto }
                    height = length { auto }
                    background = color { ColorRGBa.GRAY }
                }

                styleSheet(has class_ "padding") {
                    padding(length { 5.px })
                }

                styleSheet(has id "io-toolbar") {
                    display = Display.GRID
                    gridTemplateColumns = gridTemplate { listOf(length { 1.fr }, length { 1.fr }) }
                    gridTemplateRows = gridTemplate { listOf(length { 1.fr }) }
                    columnGap = length { 5 }

                    child(has type "button") {
                        margins(length { 0 })
                        padding(length { 0 })
                    }
                }
                // </editor-fold>

                layout {
                    div("grid", "padding") {
                        style {
                            display = Display.GRID
                            gridTemplateColumns = gridTemplate {
                                listOf(
                                    length { minmax(200.px, 1.fr) },
                                    length { 3.fr }
                                )
                            }
                            gridTemplateRows = gridTemplate { listOf(length { 1.fr }, length { 32 }) }
                            columnGap = length { 5 }
                            rowGap = length { 5 }
                            width = length { 100.percent }
                            height = length { 100.percent }
                        }

                        // <editor-fold desc="UI for model parameters">
                        uiForParameters(model).style {
                            display = Display.FLEX
                            flexDirection = FlexDirection.Column
                            rowGap = length { 5 }
                            background = color { ColorRGBa.GRAY }
                        }
                        // </editor-fold>

                        canvas("cell") {
                            style {
                                gridRow = gridPopulation { 2.rows }
                            }
                            draw { drawer ->
                                drawer.clear(ColorRGBa.BLACK)
                                drawer.fill = model.color
                                drawer.bounds.grid(model.circleCount, 1).flatten().forEach {
                                    drawer.circle(it.position(model.position), model.radius)
                                }
                            }
                        }

                        //<editor-fold desc="UI for model IO">
                        div {
                            id = "io-toolbar"
                            button {
                                label = "Load settings"
                                events.clicked.listen {
                                    openFileDialog(supportedExtensions = listOf("JSON" to listOf("json"))) {
                                        val newModel = Model.loadFromJson(it)
                                        model.copyFrom(newModel)
                                    }
                                }
                            }

                            button {
                                label = "Save settings"
                                events.clicked.listen {
                                    saveFileDialog(supportedExtensions = listOf("JSON" to listOf("json"))) {
                                        Model.saveToJson(it, model)
                                    }
                                }
                            }
                        }
                        //</editor-fold>
                    }
                }
            }
            extend(cm)
        }
    }
}