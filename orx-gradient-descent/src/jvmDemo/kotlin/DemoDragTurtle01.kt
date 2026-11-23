import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The path of a turtle that walks 40 steps, each step a stride longer than the one before, and turns by an angle
 * after every step. Dragging any corner of the path changes the stride and the angle, see [DragParameters], which
 * gives spirals, stars and everything in between.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val drag = extend(DragParameters(doubleArrayOf(8.0, 2.15), drawer.bounds.center) { (stride, turn) ->
            var position = Vector2.ZERO
            var heading = 0.0
            listOf(position) + (1..40).map { i ->
                position += Vector2(cos(heading), sin(heading)) * (i * stride)
                heading += turn
                position
            }
        })
        extend {
            drawer.translate(drag.origin)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.8)
            drawer.strokeWeight = 3.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineJoin = LineJoin.ROUND
            drawer.lineStrip(drag.points(drag.parameters))
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer, ColorRGBa.PINK.opacify(0.6), ColorRGBa.WHITE)
        }
    }
}
