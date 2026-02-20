import org.openrndr.Hit
import org.openrndr.WindowConfiguration
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.h1
import org.openrndr.panel.elements.h3
import org.openrndr.panel.elements.slider
import org.openrndr.panel.elements.style
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.FlexDirection
import org.openrndr.panel.style.background
import org.openrndr.panel.style.color
import org.openrndr.panel.style.columnGap
import org.openrndr.panel.style.defaultStyles
import org.openrndr.panel.style.display
import org.openrndr.panel.style.flexDirection
import org.openrndr.panel.style.height
import org.openrndr.panel.style.length
import org.openrndr.panel.style.padding
import org.openrndr.panel.style.percent
import org.openrndr.panel.style.textAlign
import org.openrndr.panel.style.textHorizontalAlign
import org.openrndr.panel.style.textVerticalAlign
import org.openrndr.panel.style.width
import org.openrndr.shape.Rectangle
import org.openrndr.window

fun main() = application {
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
                if (v in Rectangle(
                        0.0,
                        0.0,
                        200.0,
                        24.0
                    )
                ) Hit.DRAG else Hit.NORMAL
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
            }
            val cm = ControlManager()
            cm.document = document
            extend(cm)
        }

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