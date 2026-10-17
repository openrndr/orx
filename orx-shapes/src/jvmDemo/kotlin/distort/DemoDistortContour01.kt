package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated demonstration on how
 * [`ShapeContour.distort`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/distort.html)
 * can be used to distort a contour.
 *
 * The program creates a contour with a 24-point regular star centered on the window.
 * On every animation frame, the program distorts that contour with sines and cosines of the time in seconds.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            //val c = Circle(drawer.bounds.center, 200.0).contour
            val c = regularStar(24, 100.0, 300.0, drawer.bounds.center)

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                val dc = c.distort {
                    Vector2(
                        it.x + cos(seconds * Math.PI * 2.0 * 0.1 + it.y * 0.05) * 15.0,
                        it.y + sin(seconds * Math.PI * 2.0 * 0.1 + it.x * 0.05) * 15.0
                    )
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