package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.intersection

/**
 * Demonstrate rectangle-rectangle intersection
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val h = drawer.bounds.offsetEdges(-100.0, -10.0)
            val v = drawer.bounds.offsetEdges(-10.0, -100.0)

            extend {
                drawer.clear(ColorRGBa.WHITE)

                /**
                 * Find intersection
                 */
                val i = h.intersection(v)
                drawer.fill = ColorRGBa.RED
                drawer.rectangle(h)

                drawer.fill = ColorRGBa.BLUE
                drawer.rectangle(v)

                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangle(i)
            }
        }
    }
}