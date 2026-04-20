package assignment

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.assignment.linearSumAssignment
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.noise.scatter
import kotlin.math.min
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        // generate two sets of points
        var points1 = drawer.bounds.offsetEdges(-20.0).scatter(10.0, random = Random(0))
        var points2 = drawer.bounds.offsetEdges(-20.0).scatter(10.0, random = Random(1))

        // take the minimum size of the two sets and equalize the set sizes
        val minSize = min(points1.size, points2.size)
        points1 = points1.take(minSize)
        points2 = points2.take(minSize)

        val costMatrix = Matrix(points1.size, points2.size)

        // build a cost matrix by calculating the distance between each pair of points
        for (j in 0 until minSize) {
            for (i in 0 until minSize) {
                costMatrix[i, j] = points1[i].distanceTo(points2[j])
            }
        }

        // find a linear sum assignment using the cost matrix
        val assignment = linearSumAssignment(costMatrix)

        extend {
            drawer.clear(ColorRGBa.PINK)
            for (i in 0 until minSize) {
                val point = points1[assignment[0][i]]
                val point2 = points2[assignment[1][i]]
                drawer.lineSegment(point, point2)
            }
            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points1, 3.0)

            drawer.fill = ColorRGBa.BLACK
            drawer.circles(points2, 3.0)
        }
    }
}