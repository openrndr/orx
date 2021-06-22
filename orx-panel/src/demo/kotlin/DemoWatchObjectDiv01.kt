import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*


suspend fun main() = application {
    configure {
        width = 900
        height = 720
    }
    // A very simple state
    class State {
        var x = 0
        var y = 0
        var z = 0
    }
    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        val programState = State()
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

                slider {
                    label = "x"
                    precision = 0
                    bind(programState::x)
                }

                slider {
                    label = "y"
                    precision = 0
                    bind(programState::y)
                }

                watchObjectDiv("matrix", watchObject = object {
                    // for primitive types we have to use property references
                    val x = programState::x
                    val y = programState::y
                }) {
                    for (y in 0 until watchObject.y.get()) {
                        div("row") {
                            for (x in 0 until watchObject.x.get()) {
                                button() {
                                    label = "$x, $y"
                                }
                            }
                        }
                    }
                }
            }
        }
        extend(cm)
    }
}

