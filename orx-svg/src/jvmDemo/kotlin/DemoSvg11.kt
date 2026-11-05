import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.ClipPath
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.clipPath
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.transforms.transform
import kotlin.math.sin

/**
 * Demonstrates clip paths and paint order:
 *
 * - a group clipped by a circle: the fills and strokes in it are clipped, with anti-aliased edges
 * - a clip path over the bounds of a shape, and a clip path with clip-rule="evenodd"
 * - nested clip paths: a band within a rectangle, clipping shapes and text; the band turns, clip paths are applied
 *   every frame
 * - paint-order="stroke": the stroke of the text and of the circle in the band is painted under the fill, the circle
 *   at the top right has the normal paint order
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/clip-paths.svg")
        // the clip path of the band: the group that is clipped by it holds the text
        val band = composition.root.findGroups().mapNotNull { it.style.clipPath as? ClipPath.Shapes }
            .first { it.id == "band" }.content.children.first()

        extend {
            // turn the band around its center
            band.transform = transform {
                translate(360.0, 330.0)
                rotate(sin(seconds) * 12.0 - 8.0)
                translate(-360.0, -330.0)
            }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
