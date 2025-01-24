package hash

import org.openrndr.application
import org.openrndr.extra.noise.shapes.hash
import org.openrndr.extra.noise.shapes.uniform
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        extend {
            val b = drawer.bounds
            val b0 = b.sub(0.0, 0.0, 0.5, 1.0).offsetEdges(-10.0)
            val b1 = b.sub(0.5, 0.0, 1.0, 1.0).offsetEdges(-10.0)

            val r = Random(0)
            for (i in 0 until 20000) {
                drawer.circle(b0.uniform(r), 2.0)
                drawer.circle(b1.hash(909, i), 2.0)

            }
        }
    }
}
