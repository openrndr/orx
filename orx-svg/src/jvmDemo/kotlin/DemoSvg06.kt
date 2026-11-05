import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.FillRule
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.fillRule
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates fill rules, which decide which parts of shapes with overlapping or self-intersecting contours are
 * filled. The same three paths are drawn with fill-rule="nonzero" (the default of SVG) on top and
 * fill-rule="evenodd" at the bottom:
 *
 * - a pentagram, of which the center is surrounded twice: filled under nonzero, empty under evenodd
 * - a square with a hole that turns the same way as the outline: filled under nonzero, a hole under evenodd
 * - a square with a hole that turns the other way: a hole under both rules
 *
 * The fill rule is set on the groups and inherited by the paths in them. Press space to swap the rules of the
 * groups.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/fill-rules.svg")
        val groups = composition.root.findGroups().filter { it.id == "nonzero" || it.id == "evenodd" }

        keyboard.keyDown.listen {
            if (it.name == "spacebar") {
                for (group in groups) {
                    group.style.fillRule = when (group.style.fillRule) {
                        FillRule.NonZero -> FillRule.EvenOdd
                        FillRule.EvenOdd -> FillRule.NonZero
                    }
                }
            }
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
