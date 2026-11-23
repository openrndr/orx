import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

/**
 * A point clamped between two limits: on the line it stays between the pink limits, 20 away from them, while the
 * free point below it follows the mouse anywhere. Dragging the clamped point moves it within the limits; dragged past
 * a limit the clamp does not change with the free point any more, so gradient descent moves the limit instead, see
 * [DragParameters]. The limits can be dragged too.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val drag = extend(DragParameters(doubleArrayOf(-150.0, 20.0, 150.0), drawer.bounds.center) { (low, free, high) ->
            val clamped = free.coerceIn(low + 20.0, (high - 20.0).coerceAtLeast(low + 20.0))
            listOf(Vector2(low, 0.0), Vector2(high, 0.0), Vector2(clamped, 0.0), Vector2(free, 80.0))
        })
        extend {
            drawer.translate(drag.origin)
            val (low, high, clamped, free) = drag.points(drag.parameters)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
            drawer.lineSegment(-340.0, 0.0, 340.0, 0.0)
            drawer.lineSegment(-340.0, 80.0, 340.0, 80.0)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.6)
            drawer.lineSegment(clamped, free)
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.circles(listOf(low, high), 7.0)
            drag.drawPoints(drawer)
        }
    }
}
