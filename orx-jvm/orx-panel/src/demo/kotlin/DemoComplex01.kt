import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

/**
 * Demonstrates how to create a UI with a drop-down menu. When an option is picked,
 * the content of a Div is replaced by a button and some sliders.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 300
        }
        program {
            val cm = controlManager { }

            cm.document = document {
                styleSheets.addAll(defaultStyles())
                body {
                    div {
                        style {
                            width = length { 100.percent }
                            height = length { 100.percent }
                            display = Display.GRID
                            gridTemplateColumns = gridTemplate { listOf(length { 1.fr }, length { 1.fr }, length { 2.fr }) }
                            gridTemplateRows = gridTemplate { listOf(length { 1.fr }) }
                        }

                        val div2 = div {
                            id = "control-div"
                            style {
                                width = length { auto }
                                height = length { auto }
                                background = color { ColorRGBa.GRAY }
                                gridRow = gridPopulation { 0 }
                                gridColumn = gridPopulation { 1 }
                                display = Display.FLEX
                                flexDirection = FlexDirection.Column
                            }
                        }

                        div {
                            style {
                                width = length { auto }
                                height = length { auto }
                                background = color { ColorRGBa.GRAY.shade(0.9) }
                                display = Display.FLEX
                                flexDirection = FlexDirection.Column
                                gridRow = gridPopulation { 0 }
                                gridColumn = gridPopulation { 0 }
                            }
                            dropdownButton {
                                style {
                                    width = length { 100.percent }

                                }
                                label = "pick thing"
                                item {
                                    label = "hi"
                                    this.events.picked.listen {
                                        println("picked hi")
                                        div2.replace {
                                            button {
                                                label = "some thing 1"
                                            }
                                        }
                                    }
                                }
                                item {
                                    label = "hi2"
                                    this.events.picked.listen {
                                        println("picked hi2")
                                        div2.replace {
                                            button {
                                                label = "some thing 2"
                                            }
                                            slider {}
                                            slider {}
                                            slider {}
                                            slider {}
                                            slider {}
                                        }
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
}