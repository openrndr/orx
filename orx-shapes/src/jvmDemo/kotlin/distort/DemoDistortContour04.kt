package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demonstrates `RectifiedContour.distort`. The difference between
 * `RectifiedContour` and `ShapeContour` is that
 * in rectified contours, the `t` parameter is length-proportional.
 * With non-rectified contours, the `t` parameter does not always advance
 * at a constant speed along the contour, but depends on the lengths of the segments.
 *
 * This demo first creates a circular rectified contour, then a distorted one
 * based on it by shifting the contour outwards and inwards along the normals
 * using the cosine of the `t` value, producing 8 "petals". The formula includes
 * the current time in `seconds`, giving the impression that the shape rotates
 * in real time, even if what actually happens is that vertices are being pushed
 * in and out based on time.
 *
 * The distorted contour is distorted again in a loop, pushing the contours
 * along the normals. Notice that this time the normals no longer point away
 * from the center of the window, but away from the "petals", creating
 * offset copies.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val circle = Circle(drawer.bounds.center, 200.0).contour
            val circleRectified = circle.rectified()
            extend {
                drawer.fill = null
                drawer.drawStyle.lineJoin = LineJoin.ROUND

                val distortedCircle = circleRectified.distort { t, position ->
                    val normal = circleRectified.normal(t)
                    position + normal * cos(t * PI * 2.0 * 8.0 + seconds * PI * 2.0 * 0.1) * 100.0
                }.rectified()

                drawer.stroke = ColorRGBa.PINK.shade(0.5)
                drawer.contour(circle)

                drawer.stroke = ColorRGBa.PINK
                drawer.contour(distortedCircle.contour)

                for (i in 1 until 4) {
                    val offsetDistortedCircle = distortedCircle.distort { t, position ->
                        val normal = distortedCircle.normal(t)
                        position + normal.normalized * (10.0 * i)
                    }

                    drawer.contour(offsetDistortedCircle)

                    // Draw the segment start points
                    //drawer.circles(ndc.contour.segments.map { it.start }, 5.0)
                }

                // Draw offset samples as circles
//                val samples = (0 until 1000).map {
//                    val t = it / 1000.0
//                    distortedCircle.position(t) + distortedCircle.normal(t) * 50.0
//                }
//                drawer.circles(samples, 5.0)
            }
        }
    }
}
