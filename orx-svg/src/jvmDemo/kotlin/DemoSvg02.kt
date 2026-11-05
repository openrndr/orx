import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates SVG gradients, which are loaded as gradient paints and drawn with shade styles:
 *
 * - the top row shows the spread methods pad, reflect and repeat, with gradients that inherit their stops from
 *   another gradient with `xlink:href`
 * - the middle row shows radial gradients: one with a focal point, one with transparent stops and one with a focal
 *   radius and the reflect spread method
 * - the bottom row shows a gradient in user space shared by a rectangle and a circle, so it continues across them,
 *   a rotated gradient and a gradient stroke
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/gradients.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
