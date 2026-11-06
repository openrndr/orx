import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.transforms.transform
import kotlin.math.sin

/**
 * Demonstrates references to other documents:
 *
 * - use elements that refer to symbols in a sprite sheet, sprites.svg; the first symbol turns
 * - image elements that show views of the sprite sheet, by the ids of view elements and by svgView()
 * - feImage drawing the sprite sheet into a filter, and an element of the document repeated by feImage and feTile
 * - a checkerboard of flooded subregions repeated by feTile
 * - a style sheet imported with @import
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/cross-file.svg")
        val star = composition.root.findGroups().first { it.id == "star" }

        extend {
            star.transform = transform {
                translate(70.0, 90.0)
                rotate(sin(seconds) * 20.0)
                translate(-70.0, -90.0)
            }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
