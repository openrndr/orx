import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.FilterEffect
import org.openrndr.extra.composition.FilterPrimitive
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.filter
import org.openrndr.extra.composition.findAll
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates SVG filters and the filter functions of CSS, applied on layers:
 *
 * - a Gaussian blur, its standard deviation changes over time
 * - a drop shadow, its offset turns around the card
 * - a halo of a text: its alpha dilated, blurred, colored and merged under the text
 * - colors rotated around the hue circle
 * - fractal noise colored by a color matrix, and turbulence displacing a text; the displacement changes over time
 * - posterized colors, a duotone made by blending, and the sepia, contrast, invert, blur and drop-shadow functions
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/filters.svg")
        // the filters of the document by their id
        val filters: Map<String, FilterEffect> = composition.root.findAll { true }
            .flatMap { it.style.filter.value }.associateBy { it.id }
        val blur = filters.getValue("soft").primitives.first() as FilterPrimitive.GaussianBlur
        val shadow = filters.getValue("lifted").primitives.first() as FilterPrimitive.DropShadow
        val hue = filters.getValue("hue").primitives.first() as FilterPrimitive.ColorMatrix
        val wobble = filters.getValue("wobble").primitives.last() as FilterPrimitive.DisplacementMap

        extend {
            blur.stdDeviation = Vector2(1.0, 1.0) * (6.0 + 5.0 * sin(seconds * 1.5))
            shadow.dx = 7.0 * cos(seconds)
            shadow.dy = 7.0 * sin(seconds)
            hue.values = FilterPrimitive.ColorMatrix.hueRotate(seconds * 60.0)
            wobble.scale = 12.0 + 10.0 * sin(seconds * 2.0)
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
