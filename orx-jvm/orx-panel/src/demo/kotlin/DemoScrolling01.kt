import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.h1
import org.openrndr.panel.style.*

/**
 * Demonstrates scrolling: elements with `overflow = Overflow.Scroll` scroll content that does not fit them.
 *
 * - the strip at the top is a row of tiles that do not shrink, it scrolls horizontally
 * - the area below is a grid that is wider and taller than the area, it scrolls both ways
 *
 * Scroll with a trackpad or with the wheels of a mouse; with shift held a vertical wheel scrolls horizontally. The
 * scrollbars show how far the content is scrolled: drag a thumb to scroll, click a track to scroll by a page. They
 * show while scrolling and while the pointer is over them, and fade out after that; the strip keeps its scrollbar with
 * `scrollbarVisibility = ScrollbarVisibility.Always`. The strip styles its scrollbar with `scrollbarColor`,
 * `scrollbarTrackColor` and `scrollbarWidth`, which are inherited.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val cm = controlManager {
            styleSheet(has type "h1") {
                marginLeft = length { 20 }
                marginTop = length { 16 }
                marginBottom = length { 8 }
            }
            styleSheet(has class_ "strip") {
                display = Display.FLEX
                flexDirection = FlexDirection.Row
                columnGap = length { 8 }
                marginLeft = length { 20 }
                width = length { 680 }
                height = length { 96 }
                padding(length { 8 })
                overflow = Overflow.Scroll
                background = color { ColorRGBa.GRAY.shade(0.3) }
                scrollbarColor = color { ColorRGBa.fromHex("#f2b134") }
                scrollbarTrackColor = color { ColorRGBa.BLACK.opacify(0.4) }
                scrollbarWidth = length { 8 }
                scrollbarVisibility = ScrollbarVisibility.Always
            }
            styleSheet(has class_ "area") {
                display = Display.GRID
                gridTemplateColumns = gridTemplate { List(12) { length { 110 } } }
                gridTemplateRows = gridTemplate { List(10) { length { 70 } } }
                columnGap = length { 8 }
                rowGap = length { 8 }
                marginLeft = length { 20 }
                width = length { 680 }
                height = length { 260 }
                padding(length { 8 })
                overflow = Overflow.Scroll
                background = color { ColorRGBa.GRAY.shade(0.3) }
            }
            styleSheet(has class_ "tile") {
                width = length { 80 }
                height = length { 80 }
                flexShrink = FlexGrow.Ratio(0.0)
            }
            styleSheet(has class_ "cell") {
                height = length { 70 }
            }

            layout {
                h1 { "a row that scrolls horizontally" }
                div("strip") {
                    for (i in 0 until 24) {
                        div("tile") {
                            style = styleSheet { background = color { ColorRGBa.fromHex("#e8574a").shade(1.0 - i / 30.0) } }
                        }
                    }
                }
                h1 { "a grid that scrolls both ways" }
                div("area") {
                    for (i in 0 until 12 * 10) {
                        div("cell") {
                            style = styleSheet {
                                background = color { ColorRGBa.fromHex("#3d5a99").mix(ColorRGBa.fromHex("#f2b134"), (i % 12) / 11.0).shade(1.0 - (i / 12) / 14.0) }
                            }
                        }
                    }
                }
            }
        }
        extend(cm)
    }
}
