package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfInverseMultiQuadratic
import org.openrndr.extra.math.rbf.rbfInverseMultiQuadraticDerivative
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2

/**
 * Demonstrates visualizing the Jacobian of a two-dimensional Radial Basis Function (RBF) interpolator
 *
 * See: [Jacobian_matrix_and_determinant](https://en.wikipedia.org/wiki/Jacobian_matrix_and_determinant)
 *
 * > At each point where a function is differentiable, its Jacobian matrix can also be thought of as describing
 * > the amount of "stretching", "rotating" or "transforming" that the function imposes locally near that point.
 *
 * This program generates a grid of 4x4 points flattened to a list, and a second list with the same points but
 * sorted using Hilbert Order.
 *
 * Then an Rbf2DInterpolator is created using `rbfInverseMultiQuadraticDerivative` to map
 * the points from the first list to the second.
 *
 * Next, points in a grid of 50 columns and 50 rows are mapped using the interpolator, revealing the smooth resulting
 * transformation, even when the interpolator was constructed using 16 points only.
 *
 * The `jacobian()` method is called at each of those 2500 locations to get a Matrix representing the local
 * X and Y axis in the distorted space.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {

        val grid = drawer.bounds.offsetEdges(-50.0).grid(4, 4)
        val points = grid.flatten().map { it.center }
        val distorted = points.hilbertOrder()

        val rbf = Rbf2DInterpolator(
            points,
            distorted.map { doubleArrayOf(it.x, it.y) }.toTypedArray(),
            1E-2,
            rbf = rbfInverseMultiQuadratic(0.0055),
            rbfDerivative = rbfInverseMultiQuadraticDerivative(0.0055)
        )

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.RED

            val res = 50
            for (j in 0 until res) {
                for (i in 0 until res) {
                    val x = i / (res - 1.0) * drawer.bounds.width
                    val y = j / (res - 1.0) * drawer.bounds.width
                    val p = Vector2(x, y)
                    val qr = rbf.interpolate(p)
                    val q = Vector2(qr[0], qr[1])

                    val mat = rbf.jacobian(p)

                    drawer.lineSegment(q - Vector2(mat[0,0], mat[1,0]) * 5.0, q + Vector2(mat[0,0], mat[1,0]) * 5.0)
                    drawer.lineSegment(q  - Vector2(mat[0,1], mat[1,1]) * 5.0, q + Vector2(mat[0,1], mat[1,1]) * 5.0)
                }
            }
        }
    }
}