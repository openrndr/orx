package rseq

import org.openrndr.application
import org.openrndr.extra.noise.rsequence.rSeq2D

/**
 * Demonstrates quasirandomly distributed 2D points. The points are generated
 * using the R2 sequence and drawn as circles with a radius of 5.0.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        extend {
            val points = (0 until 4000).map {
                rSeq2D(it) * 720.0
            }
            drawer.circles(points, 5.0)
        }
    }
}
