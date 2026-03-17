import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Display
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to create a simple UI with a button to open a secondary tool window with multiple sliders.
 *
 * A `hitTest` area at the top of the tool window makes it possible to drag it with the mouse.
 *
 * The tool window can be closed by clicking its `close` button, or by pressing the ESC key.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {

        fun createToolWindow() = window(
            WindowConfiguration(
                width = 200,
                height = 500,
                hideDecorations = true,
                resizable = true,
                utilityWindow = true
            )
        ) {
            val toolWindow = this.window
            toolWindow.hitTest = { v: Vector2 ->
                if (v in Rectangle(0.0, 0.0, 200.0, 24.0)) Hit.DRAG else Hit.NORMAL
            }
            val document = document {
                styleSheets.addAll(defaultStyles())
                body {
                    div {
                        style {
                            width = length { 100.percent }
                            height = length { 100.percent }
                            display = Display.FLEX
                            flexDirection = FlexDirection.Column
                            columnGap = length { 5 }
                            background = color { ColorRGBa.GRAY.shade(0.9) }
                        }

                        div {
                            style {
                                this.width = length { 100.percent }
                                this.height = length { 24 }
                                background = color { ColorRGBa.GRAY }
                            }
                            h3 {
                                style {
                                    textVerticalAlign = textAlign { center }
                                    textHorizontalAlign = textAlign { center }
                                }
                                "Foolbar"
                            }
                        }
                        for (i in 0 until 7) {
                            slider {
                                label = "Slider $i"
                            }
                        }
                        button {
                            label = "Close"
                            events.clicked.listen {
                                toolWindow.close()
                            }
                        }
                    }
                }
                keyboard.keyDown.listen {
                    if (it.key == KEY_ESCAPE) toolWindow.close()
                }
            }
            val cm = ControlManager()
            cm.document = document
            extend(cm)
        }

        // Main window UI
        val document = document {
            styleSheets.addAll(defaultStyles())
            body {
                div {
                    style {
                        this.width = length { 100.percent }
                        this.height = length { 100.percent }
                        padding(length { 20.px })
                    }
                    h1 {
                        "Hello world!"
                    }
                    button {
                        label = "Create tool window"
                        events.clicked.listen {
                            createToolWindow()
                        }
                    }
                }
            }
        }
        val cm = ControlManager()
        cm.document = document

        extend(cm)
    }
}