import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.Length
import org.openrndr.extra.composition.OptionalLength
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.inlineSize
import org.openrndr.extra.svg.loadSVG
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * Demonstrates the direction of text and more of vertical text:
 *
 * - right to left text: its start is on the right, text-anchor end on the left, tspans follow each other to the
 *   left, bidi-override reverses the characters
 * - text-decoration of vertical text: the underline on the left of the line, the overline on the right
 * - vertical text along a path, centered on it
 * - vertical text that wraps into columns at its inline size, which changes over time
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/directions.svg")
        val columns = composition.root.findGroups().first { it.id == "columns" }
        val frame = composition.findShapes().first { it.id == "column" }

        extend {
            val size = 130.0 + 40.0 * sin(seconds * 0.8)
            columns.style.inlineSize = OptionalLength(Length.Pixels(size))
            frame.shape = Rectangle(380.0, 300.0, 320.0, size).shape
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
