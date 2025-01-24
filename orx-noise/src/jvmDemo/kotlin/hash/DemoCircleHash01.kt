package hash

import org.openrndr.application
import org.openrndr.extra.noise.shapes.hash
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.shape.Circle
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            val b = drawer.bounds
            val b0 = b.sub(0.0, 0.0, 0.5, 1.0).offsetEdges(-10.0)
            val b1 = b.sub(0.5, 0.0, 1.0, 1.0).offsetEdges(-10.0)

            val c0 = Circle(b0.center, b0.width / 2.0)
            val c1 = Circle(b1.center, b1.width / 2.0)

            val r = Random(0)
            for (i in 0 until 2000) {
                drawer.circle(c0.uniform(r), 2.0)
                drawer.circle(c1.hash(909, i), 2.0)
            }
        }
    }
}
