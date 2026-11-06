import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates image elements that refer to other SVG documents: the documents are loaded as compositions of their
 * own, fitted into the viewport of the image following preserveAspectRatio, clipped to it, and isolated from the
 * styles of the document that refers to them. A document in a data url is loaded too.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/gallery.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
