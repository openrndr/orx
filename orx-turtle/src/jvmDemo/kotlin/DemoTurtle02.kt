/*
A simple random walk made using the turtle interface.
*/

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.turtle.turtle
import org.openrndr.math.Vector2
import kotlin.random.Random

fun main() {
    application {
        program {
            val r = Random(40)
            val contours = turtle(drawer.bounds.center + Vector2(-50.0, 50.0)) {
                for (i in 0 until 500) {
                    rotate(Double.uniform(-90.0, 90.0, r))
                    forward(Double.uniform(10.0, 40.0, r))
                }
            }
            extend {
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(contours)
            }
        }
    }
}