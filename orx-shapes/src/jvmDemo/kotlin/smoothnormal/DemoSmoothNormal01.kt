package smoothnormal

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.smoothnormal.smoothNormalLinear
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {

            val r = Rectangle.fromCenter(drawer.bounds.center, 100.0, 100.0).contour
            val rc = r.rectified()

            extend {

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(r)

                val radius = 40.0 * mouse.position.y / height

                for (i in 0 until 64) {
                    val p = rc.position(i/64.0)
                    val n = rc.smoothNormalLinear(i/64.0, radius)
                    drawer.lineSegment(p, p + n * 100.0)
                }

                for (j in 0 until 10) {
                    val offset = rc.distortUniform(sampleDistance = 1.0, errorTolerance = 0.1) { t, position ->
                        position + rc.smoothNormalLinear(t, radius) * (j*10.0)

                    }
                    drawer.contour(offset)
                }


            }
        }
    }
}