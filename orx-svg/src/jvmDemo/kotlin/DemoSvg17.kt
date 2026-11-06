import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.Length
import org.openrndr.extra.composition.OptionalLength
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.inlineSize
import org.openrndr.extra.composition.textLength
import org.openrndr.extra.svg.loadSVG
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.math.Vector2
import kotlin.math.sin

/**
 * Demonstrates text layout and the color functions of CSS:
 *
 * - SVG 2 text that wraps at its inline-size, with a line-height; the inline size changes over time
 * - text adjusted to its textLength by the space between its characters, and by stretching its glyphs; the length
 *   changes over time
 * - white-space: pre, spaces are kept
 * - oklch() hues, color-mix() in oklch and in sRGB, and lab(), hwb(), color(display-p3 ...) and lch() with alpha
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/typography2.svg")
        val groups = composition.root.findGroups().associateBy { it.id }
        val shapes = composition.findShapes().associateBy { it.id }
        val paragraph = groups.getValue("paragraph")
        val adjusted = listOf(groups.getValue("spaced"), groups.getValue("stretched"))
        val column = shapes.getValue("column")
        val end = shapes.getValue("end")

        extend {
            val width = 210.0 + 90.0 * sin(seconds * 0.7)
            paragraph.style.inlineSize = OptionalLength(Length.Pixels(width))
            column.shape = Rectangle(20.0, 40.0, width, 200.0).shape
            val length = 230.0 + 90.0 * sin(seconds)
            adjusted.forEach { it.style.textLength = OptionalLength(Length.Pixels(length)) }
            end.shape = Segment2D(Vector2(380.0 + length, 40.0), Vector2(380.0 + length, 150.0)).contour.shape
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
