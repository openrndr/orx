package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.WHITE_SMOKE
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.math.Vector2

/**
 * Demonstrates the creation of a 40 hobby curves with 10 points each.
 * The control points in all hobby curves are almost identical, varying only
 * due to a slight increase in one of the arguments of a simplex noise call.
 *
 * The program shows that minor displacements in control points can have
 * a large impact in the resulting curve.
 */
fun main() = application {
    program {
        val seed = 68040
        val curves = List(40) { n ->
            hobbyCurve(List(10) {
                Vector2(
                    simplex(seed, it * 13.3, n * 0.001) * 300.0 + 320.0,
                    simplex(seed / 2, it * 77.4, n * 0.001) * 300.0 + 240.0
                )
            }.hilbertOrder(), true)
        }
        extend {
            drawer.clear(ColorRGBa.WHITE_SMOKE)
            drawer.fill = null
            drawer.stroke = ColorRGBa.BLACK.opacify(0.3)
            drawer.contours(curves)
        }
    }
}