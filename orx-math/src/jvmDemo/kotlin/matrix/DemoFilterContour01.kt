package matrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.dehankelVector2
import org.openrndr.extra.math.matrix.hankel
import org.openrndr.extra.math.matrix.svd
import org.openrndr.extra.shapes.primitives.regularStar

import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val c = regularStar(24, 100.0, 300.0, drawer.bounds.center)
            val pts = c.segments.flatMap { it.equidistantPositions((it.length / 5.0).toInt()).dropLast(1) }

            /**
             * A Hankel matrix is a square or rectangular matrix where each ascending
             * skew-diagonal from left to right is constant. For a sequence [a, b, c, d, ...],
             * a Hankel matrix might look like:
             *   a  b  c  d
             *   b  c  d  e
             *   c  d  e  f
             *   d  e  f  g
             *
             * In signal processing and filtering, Hankel matrices are used to embed a
             * 1D time series (or point sequence) into a higher-dimensional trajectory space.
             * This embedding reveals the underlying structure and correlations in the data.
             *
             * For filtering point sequences:
             * 1. The Hankel matrix transforms the sequence into a trajectory matrix where
             *    each row is a delayed copy of the signal.
             * 2. Applying SVD (Singular Value Decomposition) decomposes this matrix into
             *    orthogonal components ranked by their contribution to the signal's variance.
             * 3. By keeping only the top-k singular values, we filter out noise and
             *    high-frequency details while preserving the dominant low-frequency shape.
             * 4. Converting back from Hankel form (dehankelVector2) reconstructs a smoothed
             *    version of the original point sequence.
             *
             * This approach is particularly powerful for contour smoothing because it's
             * data-driven: the SVD automatically identifies the most significant geometric
             * features without requiring explicit frequency cutoffs or kernel functions.
             */
            val hm = pts.hankel(30, true)
            val (u, s, v) = hm.svd(fullMatrix = false)
            val vt = v.transposed()

            extend {
                // s truncated matrix
                val st = Matrix.zerosLike(s)
                for (i in 1 until 5) {

                    for (k in 0 until i) {
                        st[0, k] = s[0, k] * if (k == 0) 1.0 else  cos(seconds + k)
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

/**
 * Approximates a matrix by calculating a reduced-rank approximation using
 * the first `k` singular values and their corresponding singular vectors.
 *
 * @param u The left singular vector matrix.
 * @param s The singular value matrix, assumed to be 1 x n.
 * @param v The right singular vector matrix.
 * @param k The number of singular values (and corresponding vectors) to be used
 *          for the approximation. Must be less than or equal to the rank of the matrices.
 * @return A matrix that represents the rank-k approximation of the original matrix.
 */
fun approximate(u: Matrix, s: Matrix, v: Matrix, k : Int): Matrix {
    val result = Matrix.zeros(u.rows, s.cols)

    for (c in 0 until k) {
        for (j in 0 until u.rows) {
            for (i in 0 until u.cols) {
                result[j, i] += u[j, c] * s[0, c] * v[c, i]
            }
        }
    }

    return result
}
