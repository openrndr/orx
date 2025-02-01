package linearrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.linearrange.uniform
import org.openrndr.math.rangeTo
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val range = drawer.bounds.offsetEdges(-300.0, -50.0) .. drawer.bounds.offsetEdges(-50.0, -300.0)
            extend {
                drawer.fill = ColorRGBa.WHITE.opacify(0.9)
                val r = Random(seconds.toInt())
                for (i in 0 until 100) {
                    drawer.rectangle(range.uniform(r))
                }
            }
        }
    }
}