package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D
import kotlin.math.cos
import kotlin.math.sin

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
                    Vector2(it.x + cos(seconds * Math.PI * 2.0 * 0.1 + it.y * 0.05) * 15.0,
                            it.y + sin(seconds * Math.PI * 2.0 * 0.1 + it.x * 0.05) * 15.0)
                }
                drawer.drawStyle.lineJoin = org.openrndr.draw.LineJoin.ROUND
                drawer.contour(dc)

                //drawer.circles(dc.segments.map { it.start }, 5.0)

                drawer.stroke = ColorRGBa.PINK.opacify(0.25)
//                drawer.segments(
//                dc.segments.flatMap {
//                    listOf(
//                    Segment2D(it.start, it.control[0]),
//                        Segment2D(it.end, it.control[1])
//                    )
//                }
//                )
            }
        }
    }
}