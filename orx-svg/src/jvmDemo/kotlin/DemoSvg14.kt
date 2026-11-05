import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.Length
import org.openrndr.extra.composition.TextNode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findAll
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.letterSpacing
import org.openrndr.extra.svg.loadSVG
import kotlin.math.sin

/**
 * Demonstrates text properties and CSS:
 *
 * - letter-spacing and word-spacing; the letter spacing of the first line changes over time
 * - text placed on its dominant baseline, and text shifted to superscripts and subscripts by baseline-shift
 * - x, y, dx, dy and rotate given for every character; the rotation of "TURNING" changes over time
 * - underlines, overlines and lines through text
 * - custom properties used with var(), with fallbacks and in presentation attributes, lengths computed with calc(),
 *   and an !important declaration that wins from the style attribute
 * - a path with a pathLength of 10, its dashes and the start offset of the text along it are in units of that length,
 *   and a circle drawn without anti-aliasing with shape-rendering="crispEdges"
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/typography.svg")
        val spaced = composition.root.findGroups().first { it.id == "spaced" }
        val turning = composition.root.findGroups().first { it.id == "turning" }
            .findAll { it is TextNode }.map { it as TextNode }

        extend {
            spaced.style.letterSpacing = Length.Pixels(6.0 + 6.0 * sin(seconds))
            turning.forEachIndexed { index, run -> run.rotate = 25.0 * sin(seconds * 2.0 + index * 0.6) }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
