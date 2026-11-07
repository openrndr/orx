import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates more of CSS in panels: a media query that stacks the columns when the window is narrow and one for the
 * color scheme, text with letter spacing, an ellipsis and decorations, an inset shadow, a rotated box that clips its
 * scrolling content by its shape, flex items with a basis, an order and their own alignment, and a list of ten thousand
 * rows of which only the rows in view are built.
 */
fun main() = application {
    configure {
        width = 900
        height = 520
        windowResizable = true
    }
    program {
        lateinit var scheme: Toggle
        val cm = controlManager {
            css("""
                :root { --surface: #2a2d33; --text: #e8e8e8; --accent: #7fd1b9 }
                @media (prefers-color-scheme: light) { :root { --surface: #e9e6df; --text: #22252b } }
                .page { display: flex; flex-direction: row; gap: 16px; padding: 16px; width: 100%; height: 100% }
                @media (max-width: 700px) { .page { flex-direction: column } }

                .column { display: flex; flex-direction: column; gap: 10px; flex: 1; flex-basis: 0; align-items: stretch }
                .card { padding: 12px; border-radius: 8px; background-color: var(--surface); color: var(--text) }
                .spaced { letter-spacing: 3px; text-decoration: underline }
                .cut { width: 180px; white-space: nowrap; text-overflow: ellipsis }
                .struck { text-decoration: line-through; color: #b05050 }
                .inset { height: 60px; box-shadow: inset 0 4px 12px rgb(0 0 0 / 0.6) }

                .row { display: flex; flex-direction: row; gap: 6px; height: 50px; align-items: start }
                .chip { flex-basis: 60px; height: 24px; background-color: var(--accent); border-radius: 12px }
                .chip.last { order: -1; align-self: end; background-color: #feb47b }

                .rotated {
                    width: 260px; height: 120px; overflow: scroll; transform: rotate(6deg);
                    background: linear-gradient(to bottom, #3a4150, #20242c);
                }
                .stripe { width: 100%; height: 30px; margin: 6px; background-color: var(--accent) }
                .stripe p { white-space: nowrap; color: white; font-size: 16px }

                virtual-list { height: 220px; background-color: var(--surface); border-radius: 8px }
                .virtual-row { padding-left: 10px; color: var(--text) }
                .virtual-row:nth-child(even) { background-color: rgb(255 255 255 / 0.05) }
            """)
            layout {
                div("page") {
                    div("column") {
                        div("card", "spaced") { p { "Letter spacing" } }
                        div("card", "cut") { p { "A label that is far too long for its box" } }
                        div("card", "struck") { p { "Struck through" } }
                        div("card", "inset") { p { "Inset shadow" } }
                        div("row") {
                            div("chip") { }
                            div("chip") { }
                            div("chip", "last") { }
                        }
                    }
                    div("column") {
                        div("card", "rotated") {
                            for (i in 0 until 8) div("stripe") { p { "Stripe $i, with text that is clipped by the rotated box" } }
                        }
                        virtualList((1..10_000).toList(), 24.0) { item, _ ->
                            p { "Row $item" }
                        }
                        scheme = toggle { label = "light" }
                    }
                }
            }
        }
        extend(cm)
        // the toggle switches the color scheme that media queries see
        scheme.events.valueChanged.listen { cm.colorScheme = if (it.newValue) "light" else "dark" }
        extend {
            drawer.clear(if (cm.colorScheme == "light") ColorRGBa.fromHex("#f6f4ef") else ColorRGBa.fromHex("#1b1d21"))
        }
    }
}
