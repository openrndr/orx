package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.description
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Rectangle
import kotlin.math.cos

fun main() {
    application {
        program {

            val c = Rectangle.fromCenter(drawer.bounds.center, 200.0, 200.0).contour.rectified()
            extend {
                var p = c.position(0.0)
                val d = c.distort { t, position ->
                    position + c.normal(t) * cos(t*Math.PI*14.0) * 20.0
                }
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(d)

            }
        }
    }
}