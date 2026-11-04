import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates drawing an SVG document with gradients: linear and radial gradients in object bounding box and user
 * space units, with spread methods, a focal point, a gradient that inherits its stops from another one and is
 * rotated, stop opacity and a gradient stroke.
 *
 * Gradients are parsed into gradient paints of the composition, which are drawn with paint shade styles.
 */
fun main() = application {
    configure {
        width = 600
        height = 400
    }
    program {
        val composition = loadSVG("orx-composition/src/jvmDemo/resources/gradients.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
