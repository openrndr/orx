package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.MEDIUM_PURPLE
import org.openrndr.extra.shapes.utilities.splitAt
import org.openrndr.shape.Circle

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {

        val c = Circle(drawer.bounds.center, 300.0).contour
        val cs = c.splitAt(listOf(1.0/3.0, 2.0/3.0))
        extend {
            drawer.strokeWeight = 5.0

            drawer.stroke = ColorRGBa.PINK
            drawer.contour(cs[0])
            drawer.stroke = ColorRGBa.MEDIUM_PURPLE
            drawer.contour(cs[1])
            drawer.stroke = ColorRGBa.RED
            drawer.contour(cs[2])

        }
    }
}
