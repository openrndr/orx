import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import java.io.File

@Serializable
private class Model {
    var radius = 100.0
    var color = ColorRGBa.PINK

    companion object {
        fun loadFromJson(file: File): Model {
            return Json.decodeFromString<Model>(file.readText())
        }

        fun saveToJson(file: File, model: Model) {
            file.writeText(Json.encodeToString(model))
        }
    }
}

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

                styleSheet(has class_ "cell") {
                    width = length { auto }
                    height = length { auto }
                    background = color { ColorRGBa.GRAY }
                }

                styleSheet(has class_ "padding") {
                    paddingLeft = length { 5 }
                    paddingTop = length { 5 }
                    paddingRight = length { 5 }
                    paddingBottom = length { 5 }
                }

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

                        div {
                            id = "sidebar"
                            style {
                                display = Display.GRID
                                gridTemplateColumns = gridTemplate { length { 1.fr } }
                                gridTemplateRows = gridTemplate { length { 50 } }
                                rowGap = length { 5 }
                                background = color { ColorRGBa.GRAY }
                            }

                            slider {
                                range = Range(0.0, 400.0)
                                label = "Radius"
                                bind(model::radius)
                            }

                            colorpickerButton {
                                style = style {
                                    width = length { 100.percent }
                                }
                                label = "Color"
                                bind(model::color)
                            }
                        }
                        canvas("cell") {
                            style {
                                gridRow = gridPopulation { 2.rows }
                            }
                            draw { drawer ->
                                drawer.clear(ColorRGBa.BLACK)
                                drawer.fill = model.color
                                drawer.circle(drawer.bounds.center, model.radius)
                            }
                        }
                        div {
                            id = "io-toolbar"
                            style {
                                display = Display.GRID
                                gridTemplateColumns = gridTemplate { listOf(length { 1.fr }, length { 1.fr }) }
                                gridTemplateRows = gridTemplate { listOf(length { 1.fr }) }
                                columnGap = length { 5 }

                                and(has type "button") {
                                    background = color { ColorRGBa.PINK }
                                }
                            }
                            button {
                                label = "Load settings"
                                events.clicked.listen {
                                    openFileDialog(supportedExtensions = listOf("JSON" to listOf("json"))) {
                                        val newModel = Model.loadFromJson(it)
                                        model.radius = newModel.radius
                                        model.color = newModel.color
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
                    }
                }
            }
            extend(cm)
            extend {
                drawer.clear(ColorRGBa.BLACK)
            }
        }
    }
}