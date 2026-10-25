package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.addText
import org.openrndr.shape.Circle

/**
 * Demonstrates vertical alignment of text in shapes: the same text in three circles, aligned to the top, the center
 * and the bottom.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val face = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(16.0)

            val text = "Text flows into shapes from top to bottom. Vertical alignment moves the text down by a " +
                    "fraction of the distance it can move while all of it still fits."

            val circles = listOf(0.0, 0.5, 1.0).mapIndexed { index, verticalAlign ->
                Circle(130.0 + index * 230.0, 360.0, 110.0) to verticalAlign
            }

            val slugTextDrawer = SlugTextDrawer()
            slugTextDrawer.style = TextStyle.Defaults.copy(face = face, horizontalAlign = 0.5, language = "en")
            for ((circle, verticalAlign) in circles) {
                slugTextDrawer.addText(listOf(TextSpan(text, TextStyle(verticalAlign = verticalAlign))), listOf(circle.shape))
            }

            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK.opacify(0.5)
                    drawer.circles(circles.map { it.first })
                }
                slugTextDrawer.draw(drawer)
            }
        }
    }
}
