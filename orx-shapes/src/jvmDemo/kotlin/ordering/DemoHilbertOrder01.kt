package ordering

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

/**
 * Demonstrates the use of the `hilbertOrder` method to sort 2D points in a list of random points.
 *
 * When drawing the sorted points as a line strip, this line crosses itself fewer times than if the
 * points were drawn in a random order (sometimes zero crossings, depending on the number and layout of the points).
 *
 * The Hilbert curve (also known as the Hilbert space-filling curve) is a continuous fractal
 * space-filling curve first described by the German mathematician David Hilbert in 1891
 * https://en.wikipedia.org/wiki/Hilbert_curve
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = drawer.bounds.offsetEdges(-20.0).uniform(40, Random(0))
        val sortedPoints = points.hilbertOrder(bits = 16, scale = 1.0)
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.RED
            drawer.lineStrip(sortedPoints)
        }
    }
}
