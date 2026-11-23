import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Inverse kinematics: an arm of six links, of which the parameters are the angles of the joints, each relative to the
 * link before. Dragging the tip, or any joint, turns the joints such that it follows the mouse, see [DragParameters];
 * damping makes the joints that have to turn least do the work, and every joint but the shoulder bends at most 135°
 * either way, which the arm then works around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val lengths = listOf(120.0, 100.0, 80.0, 65.0, 50.0, 40.0)
        val base = Vector2(0.0, 300.0)
        val limit = 0.75 * PI

        /** the joints after the base, the last one the tip */
        fun joints(angles: DoubleArray): List<Vector2> {
            var position = base
            var heading = -PI / 2
            return lengths.indices.map { i ->
                heading += if (i == 0) angles[i] else angles[i].coerceIn(-limit, limit)
                position += Vector2(cos(heading), sin(heading)) * lengths[i]
                position
            }
        }

        val drag = extend(DragParameters(doubleArrayOf(0.0, 0.4, 0.4, 0.3, 0.3, 0.2), drawer.bounds.center) { joints(it) })
        drag.damping = 1000.0
        extend {
            drawer.translate(drag.origin)
            // how far the arm reaches
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.15)
            drawer.circle(base, lengths.sum())
            val points = listOf(base) + drag.points(drag.parameters)
            drawer.lineCap = LineCap.ROUND
            for (i in lengths.indices) {
                drawer.stroke = ColorRGBa.WHITE.mix(ColorRGBa.PINK, i / (lengths.size - 1.0))
                drawer.strokeWeight = 18.0 - 2.4 * i
                drawer.lineSegment(points[i], points[i + 1])
            }
            drawer.strokeWeight = 1.0
            drawer.fill = ColorRGBa.GRAY
            drawer.stroke = null
            drawer.rectangle(base.x - 40.0, base.y, 80.0, 20.0)
            drag.drawPoints(drawer, ColorRGBa.BLACK, ColorRGBa.WHITE)
        }
    }
}
