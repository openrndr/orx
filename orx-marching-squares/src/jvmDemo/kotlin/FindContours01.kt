import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = ColorRGBa.PINK
                fun f(v: Vector2) = v.distanceTo(drawer.bounds.center) - 200.0
                val contours = findContours(::f, drawer.bounds, 16.0)
                drawer.fill = null
                drawer.contours(contours)
            }
        }
    }
}