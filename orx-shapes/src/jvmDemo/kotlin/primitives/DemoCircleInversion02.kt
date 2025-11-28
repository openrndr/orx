package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.invert
import org.openrndr.extra.shapes.primitives.invertConformal
import org.openrndr.math.Polar
import org.openrndr.shape.Circle

/**
 * Demonstrates the use of the `Circle`'s `.invertConformal()` method:
 * a special type of circle inversion that preserves tangency
 * between circles. If two circles are tangent, their images
 * under conformal inversion will also be tangent.
 *
 * The program calculates a moving circle (`mc`) traveling around the
 * center of the screen.
 *
 * It then calculates a grid of 10x10 circles covering the window
 * area. Those circles are inverted using `.invertConformal()`
 * against `mc`.
 *
 * This calculation is performed twice: the first pass draws those
 * grid circles that contain the moving circle's center in black.
 *
 * The second pass draws grid circles that do not contain the moving
 * circle's center in white.
 */
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
                        val ci = mc.invertConformal(c)
                        when (ci) {
                            is Circle -> drawer.circle(ci)
                        }
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
                        val ci = mc.invertConformal(c)
                        when (ci) {
                            is Circle -> drawer.circle(ci)
                        }
                    }
                    // show the static grid of circles
                    //drawer.circle(c)
                }
            }

            // show the moving circle
            //drawer.stroke = ColorRGBa.PINK
            //drawer.strokeWeight = 2.0
            //drawer.fill = null
            //drawer.circle(mc)
        }
    }
}