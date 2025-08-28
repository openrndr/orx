package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how to select and alter the edges of a rectangle.
 *
 * The rectangle is a scaled-down version window bounds.
 *
 * By default, the edges of a rectangular contour are linear, so the `edge.toCubic()` method
 * is called to make it possible to bend them.
 *
 * Then various edges are selected one by one and transformed over time using operations like
 * scale, rotate, splitAt and moveBy.
 *
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour = drawer.bounds.scaledBy(0.5, 0.5, 0.5).contour
            contour = adjustContour(contour) {
                for (i in 0 until 4) {
                    selectEdge(i)
                    edge.toCubic()
                }
                selectEdge(0)
                edge.scale(0.5, 0.5)
                edge.rotate(cos(seconds * 0.5) * 30.0)

                selectEdge(1)
                edge.toCubic()
                edge.splitAt(0.5)
                edge.moveBy(Vector2(cos(seconds * 10.0) * 40.0, 0.0))

                //edge.next?.select()
                selectEdge(3)
                edge.moveBy(Vector2(0.0, sin(seconds * 10.0) * 40.0))


            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
