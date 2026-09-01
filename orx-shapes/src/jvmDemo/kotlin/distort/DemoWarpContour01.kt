package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {


            val c = Circle(drawer.bounds.center, 200.0).contour.rectified()


            extend {

                //val r = Rectangle.fromCenter(mouse.position, 100.0, 100.0).contour.rectified()
                val r = Circle(mouse.position, 50.0).contour.rectified()
                val b = drawer.bounds.horizontal(0.5).contour.rectified()
                val w = r.warp(b, c)


                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(b.contour)
                drawer.contour(c.contour)
                drawer.contour(w)
            }
        }
    }
}