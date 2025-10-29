package ordering

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

/**
 * Shows the difference between sorting the same random points in 2D (in red) and in 3D (in blue).
 *
 * To be able to sort the points in 3D, the 2D points are temporarily converted to 3D with 0.0 as the `z` component,
 * sorted, then converted back to 2D discarding the `z` component.
 *
 * Try out the alternative `mortonOrder` as well.
 *
 * Note that the `bits` argument can be either 5 or 16 in 2D, and 5 or 10 in 3D, other values are not supported.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = drawer.bounds.offsetEdges(-20.0).uniform(400, Random(0))
        val sortedPoints0 = points.hilbertOrder(bits = 16, scale = 1.0)
        val sortedPoints1 = points.map { it.xy0 }.hilbertOrder(bits = 10, scale = 1.0).map { it.xy }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.RED
            drawer.lineStrip(sortedPoints0)
            drawer.stroke = ColorRGBa.BLUE
            drawer.lineStrip(sortedPoints1)
        }
    }
}
