import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.gui.guiStyles
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.draw
import org.openrndr.panel.style.*

/**
 * Demonstrates how to customize the layout and the styles of the GUI.
 *
 * The layout places the side canvas on the left and the sidebar on the right, with the toolbar below the sidebar.
 * The building blocks `toolbar()`, `collapseBorder()`, `sidebar()` and `sideCanvas()` are arranged with the
 * regular `div {}` builder. A custom style sheet is appended to the GUI styles to restyle the toolbar.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        windowResizable = true
    }

    program {
        val appearance = GUIAppearance(baseColor = ColorRGBa.GRAY.shade(0.25))
        val gui = GUI(appearance)
        gui.compartmentsCollapsedByDefault = false

        gui.layout = {
            body.div("fullscreen") {
                sideCanvas()
                div("container") {
                    sidebar()
                    collapseBorder()
                    toolbar()
                }
            }
        }

        gui.styles = guiStyles(appearance) + styleSheet(has class_ "toolbar") {
            background = Color.RGBa(ColorRGBa.GRAY.shade(0.15))
            paddingTop = 5.px
            paddingBottom = 5.px
        }

        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 50.0

            @ColorParameter("color")
            var color = ColorRGBa.PINK
        }
        gui.add(settings)
        extend(gui)

        gui.canvas?.draw {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = settings.color
            drawer.circle(drawer.width / 2.0, drawer.height / 2.0, settings.radius)
        }
    }
}
