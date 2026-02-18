import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.*

/**
 * Demonstrates the use of grid layouts
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
            windowResizable = true
        }
        program {

            val cm = controlManager {
                styleSheet(has class_ "grid") {
                    paddingLeft = length { 10 }
                    paddingTop = length { 10 }
                    paddingRight = length { 10 }
                    paddingBottom = length { 10 }

                    display = Display.GRID
                    gridTemplateColumns = gridTemplate { listOf(length { minmax(100.px, 1.fr) }, length { 3.fr }) }
                    gridTemplateRows = gridTemplate { listOf(length { 100.0 }, length { 100.0 }, length { 100.0 }, length { 1.fr }) }
                    columnGap = 5.px
                    rowGap = 5.px
                    width = length { 100.percent }
                    height = length { 100.percent }
                }

                styleSheet(has class_ "control-grid") {
                    paddingLeft = length { 10 }
                    paddingTop = length { 10 }
                    paddingRight = length { 10 }
                    paddingBottom = length { 10 }

                    display = Display.GRID
                    gridTemplateColumns = gridTemplate { listOf(length { 1.fr }) }
                    gridTemplateRows = gridTemplate { length { 50 } }
                    columnGap = length { 5 }
                    rowGap = length { 5 }
                    background = color { ColorRGBa.RED }
                    overflow = Overflow.Scroll
                }

                styleSheet(has class_ "cell") {
                    width = length { auto }
                    height = length { auto }
                    background = color { ColorRGBa.GRAY }

                    and(has state "hover") {
                        background = color { ColorRGBa.WHITE.shade(0.6) }
                    }
                }

                styleSheet(has class_ "padding") {
                    paddingLeft = length { 5 }
                    paddingTop = length { 5 }
                    paddingRight = length { 5 }
                    paddingBottom = length { 5 }
                }

                layout {
                    div("grid") {
                        div("cell", "padding") {
                            style = StyleSheet()
                            style!!.gridColumn = gridPopulation { 2.rows }

                            h1 {
                                style = StyleSheet()
                                style!!.textHorizontalAlign = textAlign { center }
                                style!!.height = 100.percent
                                style!!.width = 100.percent
                                style!!.background = color { ColorRGBa.RED }
                                "Wide cell"
                            }
                        }
                        div("cell", "padding") {
                            style {
                                gridColumn = gridPopulation { 2.columns }
                            }
                            p {
                                "Wow such text!"
                            }
                        }

                        div("cell") {
                            slider {
                                label = "Slider 01"
                            }
                        }
                        div("cell") {
                            slider {
                                label = "Slider 02"
                            }
                        }

                        div("cell", "padding") {
                            p {
                                "Wow such text!"
                            }
                        }

                        div("cell", "control-grid") {
                            slider("cell") { label = "Slider 03" }
                            slider("cell") { label = "Slider 04" }
                            slider("cell") { label = "Slider 05" }
                            slider("cell") { label = "Slider 06" }
                            slider("cell") { label = "Slider 07" }
                            slider("cell") { label = "Slider 08" }
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