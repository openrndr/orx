package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.addText
import org.openrndr.extra.textwriter.composeText
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.difference

/**
 * Demonstrates text flowing into shapes: justified and hyphenated text fills a ring, lines next to the hole are split
 * in two, and the text continues in a rectangle with a circular cut-out.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val face = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(16.0)

            val ring = difference(Circle(250.0, 250.0, 220.0).shape, Circle(250.0, 250.0, 90.0).shape)
            val cutout = difference(Rectangle(380.0, 420.0, 300.0, 260.0).shape, Circle(380.0, 420.0, 120.0).shape)
            val shapes = listOf(ring, cutout)

            val text = "ABCD EFG HIJK LMNOP QRSTUVW XYZ IJ ij Typesetting is the composition of text for publication, display, or distribution by means " +
                    "of arranging physical type or its digital equivalents. Stored letters and other symbols are " +
                    "retrieved and ordered according to a language's orthography for visual display. "

            val slugTextDrawer = SlugTextDrawer()
            slugTextDrawer.style = TextStyle.Defaults.copy(face = face, justify = true, language = "en")
            slugTextDrawer.addText(composeText { repeat(6) { text(text) } }, shapes)

            extend(Camera2D())
            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK.opacify(0.5)
                    drawer.shapes(shapes)
                }
                slugTextDrawer.draw(drawer)
            }
        }
    }
}
