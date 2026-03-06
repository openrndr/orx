import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.elements.Div
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.dropdownButton
import org.openrndr.panel.elements.item
import org.openrndr.panel.elements.slider
import org.openrndr.panel.elements.style
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.FlexDirection
import org.openrndr.panel.style.LinearDimension.Companion.fr
import org.openrndr.panel.style.background
import org.openrndr.panel.style.color
import org.openrndr.panel.style.defaultStyles
import org.openrndr.panel.style.display
import org.openrndr.panel.style.flexDirection
import org.openrndr.panel.style.gridColumn
import org.openrndr.panel.style.gridPopulation
import org.openrndr.panel.style.gridRow
import org.openrndr.panel.style.gridTemplate
import org.openrndr.panel.style.gridTemplateColumns
import org.openrndr.panel.style.gridTemplateRows
import org.openrndr.panel.style.height
import org.openrndr.panel.style.length
import org.openrndr.panel.style.percent
import org.openrndr.panel.style.width

fun main() {
    application {
        configure {
            width = 800
            height = 600
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
                                    this.events.picked.listen { println("picked")
                                        div2.replace {
                                            button {
                                                label = "some thing 1"
                                            }
                                        }
                                    }
                                }
                                item {
                                    label = "hi2"
                                    this.events.picked.listen { println("picked")
                                        div2.replace {
                                            button {
                                                label = "some thing 2"
                                            }
                                            slider {

                                            }
                                            slider {

                                            }
                                            slider {

                                            }
                                            slider {

                                            }
                                            slider {

                                            }
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