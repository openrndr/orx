import com.google.gson.Gson
import org.openrndr.application
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import java.io.File

// -- these have to be top-level classes or Gson will silently fail.
private class ConfigItem {
    var value: Double = 0.0
}

private class ProgramState {
    var rows = 1
    var columns = 1
    val matrix = mutableListOf(mutableListOf(ConfigItem()))

    fun copyTo(programState: ProgramState) {
        programState.rows = rows
        programState.columns = columns
        programState.matrix.clear()
        programState.matrix.addAll(matrix)
    }

    fun save(file: File) {
        file.writeText(Gson().toJson(this))
    }

    fun load(file: File) {
        Gson().fromJson(file.readText(), ProgramState::class.java).copyTo(this)
    }
}

fun main() = application {
    configure {
        width = 900
        height = 720
    }

    program {
        val programState = ProgramState()
        val cm = controlManager {
            layout {
                styleSheet(has class_ "matrix") {
                    this.width = 100.percent
                }

                styleSheet(has class_ "row") {
                    this.display = Display.FLEX
                    this.flexDirection = FlexDirection.Row
                    this.width = 100.percent

                    child(has type "slider") {
                        this.width = 80.px
                    }
                }

                button {
                    label = "save"
                    clicked {
                        saveFileDialog(supportedExtensions = listOf("JSON" to listOf("json"))) {
                            programState.save(it)
                        }
                    }
                }

                button {
                    label = "load"
                    clicked {
                        openFileDialog(supportedExtensions =  listOf("JSON" to listOf("json"))) {
                            programState.load(it)
                        }
                    }
                }

                slider {
                    label = "rows"
                    precision = 0
                    bind(programState::rows)

                    events.valueChanged.listen {
                        while (programState.matrix.size > programState.rows) {
                            programState.matrix.removeAt(programState.matrix.size - 1)
                        }
                        while (programState.matrix.size < programState.rows) {
                            programState.matrix.add(MutableList(programState.columns) { ConfigItem() })
                        }
                    }
                }

                slider {
                    label = "columns"
                    precision = 0
                    bind(programState::columns)
                    events.valueChanged.listen {
                        for (row in programState.matrix) {
                            while (row.size > programState.columns) {
                                row.removeAt(row.size - 1)
                            }
                            while (row.size < programState.columns) {
                                row.add(ConfigItem())
                            }
                        }
                    }
                }

                watchListDiv("matrix", watchList = programState.matrix) { row ->
                    watchListDiv("row", watchList = row) { item ->
                        this.id = "some-row"
                        slider {
                            label = "value"
                            bind(item::value)
                        }
                    }
                }
            }
        }
        extend(cm)
    }
}

