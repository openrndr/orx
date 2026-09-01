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
 * Demonstrates distortion over the rectified (length proportional) t parameter
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val c = Circle(drawer.bounds.center, 200.0).contour
            val rc = c.rectified()
            val dc = rc.distort { t, position ->
                val normal = rc.normal(t)
                position + normal * cos(t * PI * 2.0 * 8.0 + seconds * PI * 2.0 * 0.1) * 100.0
            }.rectified()
            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = LineJoin.ROUND
                drawer.contour(dc.contour)

                for (i in 1 until 2) {
                    val ndc = dc.distort { t, position ->
                        val normal = dc.normal(t)
                        position + normal.normalized * (10.0 * i)
                    }

                    drawer.contour(ndc)
                }
                //drawer.circles(ndc.contour.segments.map { it.start }, 5.0)

                val samples =(0 until 1000).map {
                    val t = it / 1000.0
                    dc.position(t) + dc.normal(t) * 50.0
                }
                //drawer.circles(samples, 5.0)
            }
        }
    }
}
