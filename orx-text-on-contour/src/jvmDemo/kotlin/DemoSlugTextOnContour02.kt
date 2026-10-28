import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.slugTextOnContour
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.shape.Circle

/**
 * Demo Functionality includes:
 * - Placing text on a circular contour with different baseline offsets.
 * - A positive offset moves the text away from the contour towards its ascenders, a negative offset moves it the
 *   other way. Glyph spacing is measured on the offset baseline, so the text keeps its natural spacing.
 * - A superscript placed with [TextStyle.baselineShiftInEm].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(24.0)

        val slugMap = SlugMap(
            colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
            colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
        )
        val slugGlyphMap = SlugGlyphMap(slugMap)
        val slugDrawer = SlugDrawer()

        val circle = Circle(drawer.bounds.center, 200.0)
        val contour = circle.contour.rectified()

        val offsets = listOf(0.0 to ColorRGBa.WHITE, 30.0 to ColorRGBa.PINK, -30.0 to ColorRGBa.GRAY)

        extend {
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK.opacify(0.5)
            drawer.circle(circle)

            for ((offset, color) in offsets) {
                val spans = listOf(
                    TextSpan("baseline offset $offset, E = mc", TextStyle(fill = color)),
                    TextSpan("2", TextStyle(fill = color, sizeInEm = 0.6, baselineShiftInEm = 0.4))
                )
                slugTextOnContour(
                    drawer, slugDrawer, slugGlyphMap, TextStyle.Defaults.copy(face = face), spans, contour,
                    startT = 0.6, baselineOffset = offset
                )
            }
        }
    }
}
