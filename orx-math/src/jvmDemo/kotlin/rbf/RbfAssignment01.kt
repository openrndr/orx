package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.assignment.linearSumAssignment
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfInverseQuadratic
import org.openrndr.extra.math.rbf.rbfInverseQuadraticDerivative
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.hull.convexHull
import org.openrndr.extra.shapes.hull.convexHullSet
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import kotlin.math.exp
import kotlin.math.min
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
        // generate two sets of points
        var points1 = drawer.bounds.offsetEdges(-30.0).scatter(20.0, random = Random(0))
        var points2 = drawer.bounds.offsetEdges(-30.0).scatter(20.0, random = Random(1))

        // take the minimum size of the two sets and equalize the set sizes
        val minSize = min(points1.size, points2.size)
        points1 = points1.take(minSize)
        points2 = points2.take(minSize)


        val hull1 = points1.convexHull()
        val hull1Set = points1.convexHullSet()

        val costMatrix = Matrix(points1.size, points2.size)

        // build a cost matrix by calculating the distance between each pair of points
        for (j in 0 until minSize) {
            for (i in 0 until minSize) {
                costMatrix[i, j] = points1[i].distanceTo(points2[j])
            }
        }

        // find a linear sum assignment using the cost matrix
        val assignment = linearSumAssignment(costMatrix)

        val points = (0 until minSize).map {
            points1[assignment[0][it]]
        }

        val values = (0 until minSize).map {
            val from  = points1[assignment[0][it]]
            if (hull1Set.contains(from)) {
                println("yo hull")
                from.toDoubleArray()
            } else {
                points2[assignment[1][it]].toDoubleArray()
            }
        }.toTypedArray()


        val scale = 0.01
        val rbf = Rbf2DInterpolator(
            points,
            values,
            0.00002,
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
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.contour(hull1)
        }
    }
}