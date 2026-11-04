import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates gradients on text: a gradient over the bounds of a text element, a tspan with a gradient in user
 * space, a centered text and text along a path with gradient fills and strokes.
 *
 * Gradients over the bounds of text span the whole text element, like in browsers: the bounds are the union of the
 * cells of all its glyphs, also when the text is placed along a path.
 */
fun main() = application {
    configure {
        width = 600
        height = 400
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-composition/src/jvmDemo/resources/text-gradients.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
