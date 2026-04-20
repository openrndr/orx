package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfInverseQuadratic
import org.openrndr.extra.math.rbf.rbfInverseQuadraticDerivative
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import kotlin.math.exp
import kotlin.random.Random

/**
 * Demonstrates drawing a distorted grid using a two-dimensional Radial Basis Function (RBF) interpolator
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val grid = drawer.bounds.offsetEdges(-50.0).grid(10, 10)

        val r = Random(0)
        val points = grid.flatten().map { it.center }
        val distorted = points.map {
            val d = drawer.bounds.center.distanceTo(it)
            it + Vector2.uniformRing(exp(-d * 0.01) * 10.0, exp(-d * 0.01) * 50.0, r)
        }

        val scale = 0.0055 / 1.0
        val rbf = Rbf2DInterpolator(
            points,
            distorted.map { doubleArrayOf(it.x, it.y) }.toTypedArray(),
            0.0002,
            rbf = rbfInverseQuadratic(scale),
            rbfDerivative = rbfInverseQuadraticDerivative(scale)
        )

        extend {
            drawer.clear(ColorRGBa.PINK)
            val res = 50
            val sres = res * 10

            for (j in 0 until res) {
                val y = j / (res - 1.0) * drawer.bounds.height
                val points = mutableListOf<Vector2>()
                for (i in 0 until sres) {
                    val x = i / (sres - 1.0) * drawer.bounds.width
                    val p = Vector2(x, y)
                    val qr = rbf.interpolate(p)
                    val q = Vector2(qr[0], qr[1])

                    points.add(q)
                }
                drawer.lineStrip(points)
            }

            for (i in 0 until res) {
                val x = i / (res - 1.0) * drawer.bounds.height
                val points = mutableListOf<Vector2>()
                for (j in 0 until sres) {
                    val y = j / (sres - 1.0) * drawer.bounds.width
                    val p = Vector2(x, y)
                    val qr = rbf.interpolate(p)
                    val q = Vector2(qr[0], qr[1])

                    points.add(q)
                }
                drawer.lineStrip(points)
            }
        }
    }
}