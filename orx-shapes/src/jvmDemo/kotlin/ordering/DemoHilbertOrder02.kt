package ordering

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.shapes.ordering.mortonOrder
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val points = drawer.bounds.offsetEdges(-20.0).uniform(400, Random(0))
            val sortedPoints0 = points.hilbertOrder(bits = 16, scale = 1.0)
            val sortedPoints1  = points.map { it.xy0 }.hilbertOrder(bits = 10, scale = 1.0).map { it.xy }
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.stroke = ColorRGBa.RED
                drawer.lineStrip(sortedPoints0)
                drawer.stroke = ColorRGBa.BLUE
                drawer.lineStrip(sortedPoints1)
            }
        }
    }
}