package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.Segment2D
import kotlin.math.cos

/**
 * A demonstration of `ShapeContour.distort` similar to DemoDistortContour01.
 *
 * Only the distortion function changes: the points of the star are rotated
 * around the center of the window. The amount of rotation oscillates
 * based on the cosine of the current time in seconds, alternating between
 * a clockwise and a counter-clockwise distortion.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            //val c = Circle(drawer.bounds.center, 200.0).contour
            val c = regularStar(12, 100.0, 300.0, drawer.bounds.center)

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                val dc = c.distort {
                    val d = it.distanceTo(drawer.bounds.center) * cos(seconds * Math.PI * 2.0 * 0.1)
                    it.rotate(d, drawer.bounds.center)
                }
                drawer.drawStyle.lineJoin = LineJoin.ROUND
                drawer.contour(dc)

                // Draw the segment start points
                //drawer.circles(dc.segments.map { it.start }, 5.0)

                drawer.stroke = ColorRGBa.PINK.opacify(0.25)

                // Draw the Bézier controls
//                drawer.segments(
//                    dc.segments.flatMap {
//                        listOf(
//                            Segment2D(it.start, it.control[0]),
//                            Segment2D(it.end, it.control[1])
//                        )
//                    }
//                )
            }
        }
    }
}