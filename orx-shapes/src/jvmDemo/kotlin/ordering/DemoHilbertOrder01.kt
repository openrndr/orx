package ordering

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

fun main() {
    application {
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
}