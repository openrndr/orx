package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.primitives.Arc
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.invert
import org.openrndr.math.Polar
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {

            val p = Polar(seconds * 36.0, 100.0).cartesian + drawer.bounds.center
            val c = Circle(p, 180.0)
            for (i in 0 until 10) {
                val s = sin(seconds + i) * 0.25 + 0.25
                drawer.fill = null
                val ls = drawer.bounds.horizontal((i + 0.5) / 10.0).sub(0.5-s,0.5+s)
                drawer.stroke = ColorRGBa.PINK

                val cir = c.invert(ls)
                when (cir) {
                    is Circle -> drawer.circle(cir)
                    is Arc -> drawer.contour(cir.contour)
                    is LineSegment -> drawer.lineSegment(cir)
                    else -> error("unsupported result")
                }
            }
            drawer.isolated {
                val pts = drawer.bounds.grid(10, 10).flatten().map {
                    c.invert(it.center)
                }
                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = null
                drawer.circles(pts, 5.0)

            }
            for (i in 0 until 10) {
                drawer.fill = null
                val s = cos(seconds + i) * 0.25 + 0.25
                val ls = drawer.bounds.vertical((i + 0.5) / 10.0).sub(0.5-s,0.5+s)
                drawer.stroke = ColorRGBa.PINK
                val cir = c.invert(ls)
                when (cir) {
                    is Circle -> drawer.circle(cir)
                    is Arc -> drawer.contour(cir.contour)
                    is LineSegment -> drawer.lineSegment(cir)
                    else -> error("unsupported result")
                }
            }
        }
    }
}
