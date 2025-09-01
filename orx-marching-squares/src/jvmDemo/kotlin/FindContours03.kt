import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = ColorRGBa.PINK
            drawer.fill = null
            fun f(v: Vector2): Double {
                val p = v + Vector2(cos(v.y * 0.1 + seconds) * 40.0, sin(v.x * 0.1 + seconds) * 40.0)
                return cos((p.distanceTo(drawer.bounds.center) / 720.0) * 12 * PI)
            }

            val contours = findContours(::f, drawer.bounds.offsetEdges(-2.0), 4.0)
            drawer.contours(contours)
        }
    }
}
