import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.DashArray
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.pixels
import org.openrndr.extra.composition.strokeDashArray
import org.openrndr.extra.composition.strokeDashOffset
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates dashed strokes (stroke-dasharray and stroke-dashoffset): dashes and gaps, a pattern with an odd number
 * of lengths, which repeats, an offset, dots made by dashes of length zero with round caps, dashes around closed shapes
 * and along curves, and a dashed stroke with a gradient.
 *
 * The dash offsets of all dashed shapes are animated, which makes the dashes march along the shapes.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/dashes.svg")
        val dashed = composition.root.findShapes().filter { it.effectiveStyle.strokeDashArray is DashArray.Lengths }
        val offsets = dashed.associateWith { it.effectiveStyle.strokeDashOffset.value }

        extend {
            for (shape in dashed) {
                shape.style.strokeDashOffset = (offsets.getValue(shape) - seconds * 20.0).pixels
            }
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
