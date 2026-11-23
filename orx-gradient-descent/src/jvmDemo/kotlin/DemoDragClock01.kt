import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A clock of one parameter, the time in seconds, which runs on its own while no hand is dragged. Its three hands can
 * all be dragged, see [DragParameters]: they all set the time, so turning the minute hand once turns the hour hand a
 * twelfth and the second hand 60 times.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        /** the tip of a hand of [length] that turns once in [period] seconds, at [time] */
        fun hand(time: Double, period: Double, length: Double): Vector2 {
            val angle = time / period * 2 * PI - PI / 2
            return Vector2(cos(angle), sin(angle)) * length
        }

        val hands = listOf(43200.0 to 150.0, 3600.0 to 230.0, 60.0 to 260.0)
        val drag = extend(DragParameters(doubleArrayOf(LocalTime.now().toSecondOfDay().toDouble()), drawer.bounds.center) { (time) ->
            hands.map { (period, length) -> hand(time, period, length) }
        })
        var last = seconds
        extend {
            if (drag.dragged < 0) drag.parameters[0] += seconds - last
            last = seconds
            drawer.translate(drag.origin)
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
            drawer.circle(Vector2.ZERO, 290.0)
            drawer.lineSegments((0 until 60).flatMap { i ->
                val outer = hand(i.toDouble(), 60.0, 280.0)
                listOf(outer, outer * (if (i % 5 == 0) 0.88 else 0.95))
            })
            drawer.lineCap = LineCap.ROUND
            val tips = drag.points(drag.parameters)
            for ((i, tip) in tips.withIndex()) {
                drawer.stroke = if (i == 2) ColorRGBa.PINK else ColorRGBa.WHITE
                drawer.strokeWeight = listOf(10.0, 6.0, 2.0)[i]
                drawer.lineSegment(Vector2.ZERO, tip)
            }
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer)
        }
    }
}
