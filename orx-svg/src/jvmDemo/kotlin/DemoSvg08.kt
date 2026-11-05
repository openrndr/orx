import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates SVG patterns on text: a checkerboard in user space, stripes turned by a pattern transform, as a fill
 * and as a stroke, dots in a pattern over the bounds of a text, and text along a path with a pattern fill.
 *
 * A pattern in user space continues through all glyphs of a text; a pattern over the bounds of a text spans the whole
 * text element, so the dots of "dots in bounds" are laid out over the full width of the line.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/text-patterns.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
