import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates styling a user interface with CSS: selectors with classes, states and structural pseudo-classes,
 * shorthands, !important, and custom properties with var(). Hover the buttons to see their states.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val cm = controlManager {
            css("""
                body { --accent: #5ab0ff; --surface: #2a2d33; --space: 8px; color: #e8e8e8 }
                .toolbar { display: flex; flex-direction: row; background-color: var(--surface);
                           padding: var(--space); gap: var(--space) }
                .toolbar > button { width: 120px; background-color: #3a3f47; border: 2px solid transparent;
                                    transition: background-color 150ms ease-out }
                .toolbar > button:hover { background-color: var(--accent) }
                .toolbar > button:first-child { border-color: var(--accent) }
                .toolbar > button:nth-child(3) { background-color: #b05050 !important }
                .cards { display: flex; flex-direction: row; padding: calc(var(--space) * 2); gap: 12px }
                .cards > div { width: 200px; height: 160px; background-color: var(--surface); padding: 12px;
                               border-radius: 8px }
                .cards > div:not(.featured) { opacity: 0.6 }
                .featured { --surface: #3d4a5c; outline: 2px solid var(--accent) }
            """)
            layout {
                div("toolbar") {
                    button { label = "Accent border" }
                    button { label = "Hover me" }
                    button { label = "Important" }
                }
                div("cards") {
                    div { h3 { "Muted" } }
                    div("featured") { h3 { "Featured" } }
                    div { h3 { "Muted" } }
                }
            }
        }
        extend(cm)
        extend {
            drawer.clear(ColorRGBa.fromHex("#1b1d21"))
        }
    }
}
