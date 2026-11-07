import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates the effects of CSS in panels: gradient backgrounds, box shadows, transforms that transition when the
 * cards are hovered, animations with @keyframes, and selectors of attributes and states, like `button[label="Delete"]`
 * and `toggle:checked`.
 */
fun main() = application {
    configure {
        width = 760
        height = 420
    }
    program {
        val cm = controlManager {
            css("""
                .cards { display: flex; flex-direction: row; gap: 24px; padding: 32px }
                .card {
                    width: 200px; height: 150px; padding: 16px; border-radius: 12px;
                    box-shadow: 0 8px 24px rgb(0 0 0 / 0.45), 0 1px 3px rgb(0 0 0 / 0.6);
                    transition: transform 180ms ease-out;
                }
                .card:hover { transform: translateY(-6px) scale(1.04) }
                .sunset { background: linear-gradient(135deg, #ff7e5f, #feb47b) }
                .ocean { background: radial-gradient(circle at 30% 20%, #6dd5fa, #2980b9 60%, #1c3d5a) }
                .stripes { background: repeating-linear-gradient(45deg, #3a3f47 0, #3a3f47 10px, #2a2d33 10px, #2a2d33 20px) }
                .card h3 { color: white }

                @keyframes pulse {
                    from { transform: scale(1); opacity: 0.6 }
                    50% { transform: scale(1.15); opacity: 1 }
                    to { transform: scale(1); opacity: 0.6 }
                }
                .badge {
                    width: 18px; height: 18px; border-radius: 50%; margin: 8px 0 0 0;
                    background-color: #7fd1b9; animation: pulse 1.2s ease-in-out infinite;
                }

                .actions { display: flex; flex-direction: row; padding: 0 32px; gap: 8px }
                button[label="Delete"] { background-color: #b05050 }
                toggle:checked { color: #7fd1b9 }
            """)
            layout {
                div("cards") {
                    div("card", "sunset") { h3 { "Sunset" }; div("badge") { } }
                    div("card", "ocean") { h3 { "Ocean" } }
                    div("card", "stripes") { h3 { "Stripes" } }
                }
                div("actions") {
                    button { label = "Save" }
                    button { label = "Delete" }
                    toggle { label = "checked"; value = true }
                }
            }
        }
        extend(cm)
        extend {
            drawer.clear(ColorRGBa.fromHex("#1b1d21"))
        }
    }
}
