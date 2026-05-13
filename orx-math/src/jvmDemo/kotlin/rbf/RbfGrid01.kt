package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfInverseQuadratic
import org.openrndr.extra.math.rbf.rbfInverseQuadraticDerivative
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.exp
import kotlin.random.Random

/**
 * Demonstrates drawing a grid and a rotating rectangle both distorted using a two-dimensional Radial Basis Function (RBF) interpolator.
 *
 * The first part of the code creates the interpolator which is later used to map 2D vectors to distorted coordinates.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val grid = drawer.bounds.offsetEdges(-50.0).grid(10, 10)

        // Create collections of points and distorted points
        val r = Random(0)
        val points = grid.flatten().map { it.center }
        val distorted = points.map {
            val d = drawer.bounds.center.distanceTo(it)
            it + Vector2.uniformRing(exp(-d * 0.01) * 10.0, exp(-d * 0.01) * 50.0, r)
        }

        // Create the interpolator to map points to distorted points
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

            // make use of the interpolator

            // render horizontal grid lines
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

            // render vertical grid lines
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

            // render an interactive rotating rectangle
            val points = Rectangle.fromCenter(Vector2.ZERO, 240.0, 120.0).contour.transform(
                transform {
                    translate(mouse.position)
                    rotate(seconds * 30.0)
                }
            ).equidistantPositions(100).map {
                val p = rbf.interpolate(it)
                Vector2(p[0], p[1])
            }
            drawer.contour(ShapeContour.fromPoints(points, true))
        }
    }
}