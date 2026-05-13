package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfGaussian
import org.openrndr.extra.math.rbf.rbfGaussianDerivative
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

/**
 * Demonstrates visualizing the Jacobian of a two-dimensional Radial Basis Function (RBF) interpolator
 *
 * See: [Jacobian_matrix_and_determinant](https://en.wikipedia.org/wiki/Jacobian_matrix_and_determinant)
 *
 * > At each point where a function is differentiable, its Jacobian matrix can also be thought of as describing
 * > the amount of "stretching", "rotating" or "transforming" that the function imposes locally near that point.
 *
 * This program generates a grid of 10x10 points flattened to a list, and a second list with the same points but
 * shifted randomly up to 34 pixels away.
 *
 * Then an Rbf2DInterpolator is created using `rbfGaussianDerivative` to map
 * the points from the first list to the second.
 *
 * Next, points in a grid of 40 columns and 40 rows are mapped using the interpolator.
 *
 * The `jacobian()` method is called at each of those 1600 locations to get a Matrix representing the local
 * X and Y axis in the distorted space, and used to create short distorted horizontal and vertical line segments.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val grid = drawer.bounds.offsetEdges(-50.0).grid(10, 10)
            val points = grid.flatten().map { it.center }
            val distorted = points.map { it + Vector2.uniformRing(0.0, 34.0) }

            val rbf = Rbf2DInterpolator(
                points,
                distorted.map { doubleArrayOf(it.x, it.y) }.toTypedArray(),
                1E-2,
                rbf = rbfGaussian(0.0055),
                rbfDerivative = rbfGaussianDerivative(0.0055)
            )

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.stroke = ColorRGBa.BLACK

                val lineSegments = mutableListOf<LineSegment>()
                val res = 40
                for (j in 0 until res) {
                    for (i in 0 until res) {
                        val x = i / (res - 1.0) * drawer.bounds.width
                        val y = j / (res - 1.0) * drawer.bounds.width
                        val p = Vector2(x, y)
                        val qr = rbf.interpolate(p)
                        val q = Vector2(qr[0], qr[1])
                        val j = rbf.jacobian(p)
                        lineSegments.add(
                            LineSegment(
                                q - Vector2(j[0, 0], j[1, 0]) * 5.0,
                                q + Vector2(j[0, 0], j[1, 0]) * 5.0
                            )
                        )
                        lineSegments.add(
                            LineSegment(
                                q - Vector2(j[0, 1], j[1, 1]) * 5.0,
                                q + Vector2(j[0, 1], j[1, 1]) * 5.0
                            )
                        )
                    }
                }
                drawer.lineSegments(lineSegments)
            }
        }
    }
}