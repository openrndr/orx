package hammersley

import org.openrndr.application
import org.openrndr.extra.noise.hammersley.hammersley2D

/**
 * Demo that visualizes a 2D Hammersley point set.
 *
 * The program computes 400 2D Hammersley points mapped within the window bounds.
 * These points are visualized by rendering circles at their respective positions.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        extend {
            val points = (0 until 400).map {
                hammersley2D(it, 400) * 720.0
            }
            drawer.circles(points, 5.0)
        }
    }
}
