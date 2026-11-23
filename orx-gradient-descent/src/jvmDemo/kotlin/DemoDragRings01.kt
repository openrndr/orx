import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Two rings of ten points of a radius and an angle: the outer ring at the radius, turned by the angle, the inner ring
 * at half the radius, turned the other way. Dragging any point turns and scales both rings, see [DragParameters].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val count = 10
        val drag = extend(DragParameters(doubleArrayOf(200.0, 0.0), drawer.bounds.center) { (radius, angle) ->
            (0 until count).flatMap { i ->
                val a = angle + i * 2 * PI / count
                listOf(Vector2(cos(a), sin(a)) * radius, Vector2(cos(-a), sin(-a)) * (radius / 2))
            }
        })
        extend {
            drawer.translate(drag.origin)
            val points = drag.points(drag.parameters)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.lineSegments(points)
            drawer.lineLoop(points.filterIndexed { i, _ -> i % 2 == 0 })
            drawer.lineLoop(points.filterIndexed { i, _ -> i % 2 == 1 })
            drag.drawPoints(drawer)
        }
    }
}
