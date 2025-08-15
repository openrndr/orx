package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.invert
import org.openrndr.extra.shapes.primitives.invertConformal
import org.openrndr.math.Polar
import org.openrndr.shape.Circle

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val p = Polar(seconds * 36.0, 100.0).cartesian + drawer.bounds.center
            val mc = Circle(p, 100.0)

            // check if p is inside any of the circles
            for (j in 0 until 10) {
                for (i in 0 until 10) {
                    val c = Circle(i * width / 10.0 + width / 20.0, j * height / 10.0 + height / 20.0, 36.0)
                    if (p in c) {
                        drawer.clear(ColorRGBa.WHITE)
                        drawer.fill = ColorRGBa.BLACK
                        drawer.stroke = null
                        drawer.circle(mc.invertConformal(c))
                        break
                    }
                }

            }

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE

            for (j in 0 until 10) {
                for (i in 0 until 10) {
                    val c = Circle(i * width / 10.0 + width / 20.0, j * height / 10.0 + height / 20.0, 36.0)
                    if (p !in c) {
                        drawer.circle(mc.invertConformal(c))
                    }
                }
            }
        }
    }
}