package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.Net
import org.openrndr.shape.Circle
import kotlin.math.sin

/**
 * Shows how to create and render a [Net]: a structure
 * that connects two points with a circle in between,
 * forming a string-like shape.
 *
 * The main circle moves following an invisible infinite sign,
 * formed by a pair of sine functions. The moving circle is connected to
 * two smaller static circles via a [Net], rendered as a white
 * contour with a stroke weight 2 pixels wide.
 */
fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = ColorRGBa.PINK
            val a = drawer.bounds.position(0.7, 0.5)
            val b = drawer.bounds.position(0.3, 0.5)
            val c = Circle(
                drawer.bounds.position(
                    sin(seconds) * 0.35 + 0.5,
                    sin(seconds * 2) * 0.25 + 0.5
                ), 50.0
            )
            val net = Net(a, b, c)
            drawer.circle(a, 10.0)
            drawer.circle(b, 10.0)
            drawer.circle(c)

            drawer.strokeWeight = 2.0
            drawer.contour(net.contour)
        }
    }
}
