// Converting Catmull-Rom curves to Bezier

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.CatmulRomChain2
import org.openrndr.math.Polar
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.toContour

suspend fun main() = application {
    program {
        val points = List(6) { Polar(it * 70.0, 100.0).cartesian + drawer.bounds.center }
        val cmr = CatmulRomChain2(points, 1.0, loop = true)
        val contour = ShapeContour.fromPoints(cmr.positions(200), true)

        extend {
            drawer.run {
                clear(ColorRGBa.WHITE)
                fill = null
                stroke = ColorRGBa.BLACK
                contour(contour)
                circles(points, 5.0)

                stroke = ColorRGBa.RED
                contour(cmr.toContour())
                fill = ColorRGBa.BLACK
            }
        }
    }
}
