package matrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.dehankelVector2
import org.openrndr.extra.math.matrix.hankel
import org.openrndr.extra.math.matrix.svd
import org.openrndr.extra.shapes.primitives.regularStar

import kotlin.math.cos

/**
 * An advanced demonstration of using a Henkel matrix to smooth a star-shaped contour.
 *
 * The animated shape is drawn multiple times with varying levels of smoothing.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val c = regularStar(24, 100.0, 300.0, drawer.bounds.center)
            val pts = c.segments.flatMap { it.equidistantPositions((it.length / 5.0).toInt()).dropLast(1) }

            // Create a Hankel-like matrix
            val hm = pts.hankel(30, true)

            // Use Singular Value Decomposition (SVD)
            val (u, s, v) = hm.svd(fullMatrix = false)

            val vt = v.transposed()

            extend {
                // s truncated matrix
                val st = Matrix.zerosLike(s)
                for (i in 1 until 5) {

                    for (k in 0 until i) {
                        st[0, k] = s[0, k] * if (k == 0) 1.0 else cos(seconds + k)
                    }

                    val rm = approximate(u, st, vt, i)
                    val rp = rm.dehankelVector2(true)

                    drawer.stroke = ColorRGBa.WHITE
                    drawer.lineLoop(rp)
                }
            }
        }
    }
}
