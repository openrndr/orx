package matrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.invertMatrixCholesky
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D
import kotlin.math.pow
import kotlin.random.Random

/**
 * Demonstrate how to use the `least squares` method to fit a cubic bezier to noisy points.
 *
 * On every animation frame, 10 concentric circles are created centered on the window and converted to contours.
 * In OPENRNDR, circular contours are made ouf of 4 cubic-Bezier curves. Each of those curves is considered
 * one by one as the ground truth, then 5 points are sampled near those curves.
 * Finally, two matrices are constructed using those points and math operations are applied to
 * revert the randomization attempting to reconstruct the original curves.
 *
 * The result is drawn on every animation frame, revealing concentric circles that are more or less similar
 * to the ground truth depending on the random values used.
 *
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val r = Random(0)
            fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1)

            fun comb(a: Int, b: Int): Int {
                return factorial(a) / (factorial(b) * factorial(a - b))
            }

            fun bernstein(n: Int, i: Int, t: Double): Double {
                return comb(n, i) * t.pow(i) * (1.0 - t).pow(n - i)
            }
            extend {
                for (z in 0 until 10) {
                    val c = Circle(drawer.bounds.center, 300.0 - z * 30.0).contour
                    for (groundTruth in c.segments) {

                        val pointCount = 5
                        val A = Matrix(pointCount, 4)
                        val b = Matrix(pointCount, 2)
                        for (i in 0 until pointCount) {
                            val t = when (i) {
                                0 -> 0.0
                                pointCount - 1 -> 1.0
                                else -> Double.uniform(0.0, 1.0, r)
                            }
                            val point = groundTruth.position(t)
                            val pointRandomized = point + Vector2.uniformRing(0.0, 0.5, r)

                            A[i, 0] = bernstein(3, 0, t)
                            A[i, 1] = bernstein(3, 1, t)
                            A[i, 2] = bernstein(3, 2, t)
                            A[i, 3] = bernstein(3, 3, t)
                            b[i, 0] = pointRandomized.x
                            b[i, 1] = pointRandomized.y
                        }
                        val At = A.transposed()
                        val AtA = At * A
                        val Atb = At * b

                        val AtAI = invertMatrixCholesky(AtA)
                        val x = AtAI * Atb

                        val segment = Segment2D(
                            Vector2(x[0, 0], x[0, 1]),
                            Vector2(x[1, 0], x[1, 1]),
                            Vector2(x[2, 0], x[2, 1]),
                            Vector2(x[3, 0], x[3, 1])
                        )

                        drawer.stroke = ColorRGBa.PINK
                        drawer.segment(segment)
                    }
                }
            }
        }
    }
}