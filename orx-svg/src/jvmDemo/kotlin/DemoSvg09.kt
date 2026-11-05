import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates SVG features that documents exported by design tools use:
 *
 * - top left: a style element with CSS rules for classes, types, ids and descendants; a style attribute overrides
 *   them, as do more specific rules
 * - top right: display="none" and visibility="hidden" (a visible child of a hidden group is drawn), colors in #rgba,
 *   #rrggbbaa, rgba(), hsl() and hsla() notation, and currentColor, which takes the color property of a group
 * - middle: use elements that draw copies of a circle and of a symbol, which is fitted into the size of the use
 *   element by its view box, and a switch element, of which only the first child is drawn
 * - bottom: in a group that is scaled 4 by 2, a regular stroke scales with it, a non-scaling stroke
 *   (vector-effect="non-scaling-stroke") does not
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/features.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
