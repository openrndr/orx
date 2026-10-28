package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.shape.Rectangle

/**
 * Demonstrates hyphenation: the same text in two justified columns, on the left without hyphenation and on the right
 * hyphenated automatically by setting [TextStyle.language].
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val face = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(22.0)

            val text = "Typesetting is the composition of text for publication, display, or distribution by means of " +
                    "arranging physical type or its digital equivalents. Hyphenation allows justified text to be set " +
                    "with fewer and smaller gaps between the words, which considerably improves the appearance of " +
                    "narrow columns."

            val left = Rectangle(40.0, 40.0, 300.0, 640.0)
            val right = Rectangle(380.0, 40.0, 300.0, 640.0)

            val slugTextDrawer = SlugTextDrawer()
            slugTextDrawer.style = TextStyle.Defaults.copy(face = face, justify = true, lineHeightInEm = 1.2)
            slugTextDrawer.addText(text, left)
            slugTextDrawer.addText(text, right, TextStyle(language = "en"))

            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK.opacify(0.5)
                    drawer.rectangles(listOf(left, right))
                }
                slugTextDrawer.draw(drawer)
            }
        }
    }
}
