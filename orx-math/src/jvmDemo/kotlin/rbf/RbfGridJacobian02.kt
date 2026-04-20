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
            val res = 50
            drawer.stroke = ColorRGBa.RED

            for (j in 0 until res) {
                for (i in 0 until res) {
                    val x = i / (res - 1.0) * drawer.bounds.width
                    val y = j / (res - 1.0) * drawer.bounds.width
                    val p = Vector2(x, y)
                    val qr = rbf.interpolate(p)
                    val q = Vector2(qr[0], qr[1])

                    val j = rbf.jacobian(p)

                    drawer.stroke = ColorRGBa.RED
                    drawer.lineSegment(q - Vector2(j[0,0], j[1,0]) * 5.0, q + Vector2(j[0,0], j[1,0]) * 5.0)
                    drawer.lineSegment(q  - Vector2(j[0,1], j[1,1]) * 5.0, q + Vector2(j[0,1], j[1,1]) * 5.0)
                }
            }
        }
    }
}