import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.sin

/**
 * Demonstrates line joins and markers painted with the paints of the paths that use them:
 *
 * - the joins of SVG 2: miter, miter-clip, arcs, bevel and round, with a miter limit of 2.5. The corners become
 *   sharper and blunter over time; past the miter limit a miter is beveled, a clipped miter is cut off at the limit.
 *   Arcs joins are drawn as clipped miters, which is what they are between straight segments.
 * - markers painted with context-fill and context-stroke: arrowheads in the color of the stroke of their line, or
 *   filled with its fill and outlined with its stroke; rings in the colors of their polyline, hollow on the polyline
 *   that has no fill
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/joins.svg")
        val joins = (0..4).map { i -> composition.findShapes().first { it.id == "j$i" } }

        extend {
            // the half width of the corner, from sharp to blunt
            val spread = 25.0 + 20.0 * sin(seconds)
            val corner = ShapeContour.fromPoints(listOf(Vector2(-spread, 220.0), Vector2(0.0, 80.0), Vector2(spread, 220.0)), closed = false).shape
            joins.forEach { it.shape = corner }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
