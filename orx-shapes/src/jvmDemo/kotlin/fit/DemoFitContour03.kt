package fit

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.extrema.findLocalMaxima
import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.segmentation.segmentByCurvature
import org.openrndr.shape.ShapeContour

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val pts = drawer.bounds.scatter(5.0).hilbertOrder()


            val segments = fitCubicBeziers(pts, minPointsToSplit = 25)
            val contour = ShapeContour.fromSegments(segments, false)

            val lowCurvature = contour.segmentByCurvature(0.02, samples = 5000, minLengthFraction = 0.0001)

            val extra = lowCurvature.map {
                val rect = it.rectified()
                rect.distort { t, position ->
                    position + rect.normal(t) * 5.0
                }
            }
            val extra2 = lowCurvature.map {
                val rect = it.rectified()
                rect.distort { t, position ->
                    position + rect.normal(t) * -5.0
                }
            }

            val maxima = contour.rectified().findLocalMaxima(10000, 0.001) { t -> this.curvature(t) }




            extend {

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(contour)
                //drawer.stroke = ColorRGBa.RED
                drawer.contours(extra)
                drawer.contours(extra2)

                drawer.circles(maxima.map { it.position }, 5.0)
            }
        }
    }
}