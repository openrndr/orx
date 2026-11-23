import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A grid warped by a twist and a scale: every point turns around the center by the twist times its distance to the
 * center, and its distance is scaled. Dragging any point of the grid changes the twist and the scale, see
 * [DragParameters].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val n = 10
        val spacing = 24.0
        val drag = extend(DragParameters(doubleArrayOf(0.05, 1.0), drawer.bounds.center) { (twist, scale) ->
            (-n..n).flatMap { j ->
                (-n..n).map { i ->
                    val r = Vector2(i.toDouble(), j.toDouble()).length
                    val angle = atan2(j.toDouble(), i.toDouble()) + twist * r
                    Vector2(cos(angle), sin(angle)) * (r * scale * spacing)
                }
            }
        })
        extend {
            drawer.translate(drag.origin)
            val points = drag.points(drag.parameters)
            val size = 2 * n + 1
            drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
            for (k in 0 until size) {
                drawer.lineStrip(points.subList(k * size, (k + 1) * size))
                drawer.lineStrip((0 until size).map { points[it * size + k] })
            }
            drag.drawPoints(drawer)
        }
    }
}
