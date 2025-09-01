package matrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.invertMatrixCholesky
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.random.Random

/**
 * Demonstrate least squares method to find a regression line through noisy points
 * Line drawn in red is the estimated line, in green is the ground-truth line
 *
 * Ax = b => x = A⁻¹b
 * because A is likely inconsistent, we look for an approximate x based on AᵀA, which is consistent.
 * x̂ = (AᵀA)⁻¹ Aᵀb
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val ls = drawer.bounds.horizontal(0.5).rotateBy(cos(seconds)*45.0)

                val r = Random((seconds*10).toInt())

                val pointCount = 100
                val A = Matrix(pointCount, 2)
                val b = Matrix(pointCount, 1)
                for (i in 0 until pointCount) {

                    val p = ls.position(Double.uniform(0.0, 1.0, r))
                    val pr = p + Vector2.uniformRing(0.0, 130.0, r)

                    A[i, 0] = 1.0
                    A[i, 1] = pr.x
                    b[i, 0] = pr.y

                    drawer.circle(pr, 5.0)
                }
                val At = A.transposed()
                val AtA = At * A
                val Atb = At * b

                val AtAI = invertMatrixCholesky(AtA)
                val x = AtAI * Atb

                val p0 = Vector2(0.0, x[0, 0])
                val p1 = Vector2(720.0, x[0, 0] + x[1, 0] * 720.0)

                drawer.stroke = ColorRGBa.RED
                drawer.lineSegment(p0, p1)

                drawer.stroke = ColorRGBa.GREEN
                drawer.lineSegment(ls)
            }
        }
    }
}