package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.VectorTextDrawer
import org.openrndr.extra.textwriter.composeText

/**
 * Demonstrates [VectorTextDrawer]: text is typeset into a box and converted to glyph outlines,
 * the outlines are drawn as strokes with the segment end points marked.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val f = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(72.0)

            val vectorTextDrawer = VectorTextDrawer()
            vectorTextDrawer.style = TextStyle.Defaults.copy(face = f, horizontalAlign = 0.5, verticalAlign = 0.5)

            val spans = composeText {
                line("Vector")
                span("outlines") { sizeInEm = 1.5 }
            }
            vectorTextDrawer.addText(spans, listOf(drawer.bounds.offsetEdges(-40.0)))

            val shapes = vectorTextDrawer.shapes
            val points = shapes.flatMap { shape -> shape.contours.flatMap { contour -> contour.segments.map { it.start } } }

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.shapes(shapes)

                drawer.fill = ColorRGBa.PINK
                drawer.stroke = null
                drawer.circles(points, 3.0)
            }
        }
    }
}
