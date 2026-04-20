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
                val res = 40

                drawer.stroke = ColorRGBa.BLACK
                val lineSegments = mutableListOf<LineSegment>()
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