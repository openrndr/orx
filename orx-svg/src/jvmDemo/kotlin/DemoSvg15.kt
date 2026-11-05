import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findAll
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.mask
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.transforms.transform
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates group opacity, masks and blend modes, which draw nodes on layers of their own:
 *
 * - a group with an opacity is drawn before its opacity is applied, the overlap of its circles does not show, unlike
 *   that of the circles below it, which have an opacity of their own
 * - a luminance mask: a soft spot that moves over a group of text
 * - alpha masks in the bounds of a rectangle and of a text
 * - the sixteen blend modes of CSS: a circle blended with a gradient
 * - a disc blended with mix-blend-mode: difference, moving over everything
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/layers.svg")
        // the circle in the content of the mask
        val spot = composition.root.findAll { it.style.mask.value?.id == "spotlight" }.first()
            .style.mask.value!!.content.findShapes().first { it.id == "spot" }
        val lens = composition.findShapes().first { it.id == "lens" }

        extend {
            spot.transform = transform { translate(60.0 * cos(seconds), 40.0 * sin(seconds * 1.3)) }
            lens.transform = transform { translate(300.0 * sin(seconds * 0.4), 45.0 * sin(seconds * 0.9)) }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
