import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

/**
 * Two points of the parameters x and y, one at (x, y) and one at (y, x): dragging either one changes x and y by
 * gradient descent, see [DragParameters], so the other one follows, mirrored in the diagonal.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val drag = extend(DragParameters(doubleArrayOf(150.0, -50.0), drawer.bounds.center) { (x, y) ->
            listOf(Vector2(x, y), Vector2(y, x))
        })
        extend {
            drawer.translate(drag.origin)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.lineSegment(-300.0, -300.0, 300.0, 300.0)
            val (a, b) = drag.points(drag.parameters)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            drawer.lineSegment(a, b)
            drag.drawPoints(drawer)
        }
    }
}
