import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A crank and a piston, of one parameter, the angle of the crank: a rod joins the pin of the crank to the piston,
 * which moves up and down in its cylinder. Dragging the pin turns the crank; dragging the piston turns it too, to the
 * nearest angle that puts the piston where the mouse is, see [DragParameters]. When nothing is dragged the crank turns
 * on its own.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val axle = Vector2(0.0, 150.0)
        val crank = 70.0
        val rod = 200.0

        fun pin(angle: Double) = axle + Vector2(cos(angle), sin(angle)) * crank

        /** the piston, straight above the axle, at the length of the rod from the pin */
        fun piston(angle: Double): Vector2 {
            val p = pin(angle)
            return Vector2(axle.x, p.y - sqrt(rod * rod - (p.x - axle.x) * (p.x - axle.x)))
        }

        val drag = extend(DragParameters(doubleArrayOf(0.0), drawer.bounds.center) { (angle) ->
            listOf(pin(angle), piston(angle))
        })
        var last = seconds
        extend {
            if (drag.dragged < 0) drag.parameters[0] += (seconds - last) * 1.5
            last = seconds
            drawer.translate(drag.origin)
            val angle = drag.parameters[0]
            val pin = pin(angle)
            val piston = piston(angle)

            // the cylinder, from the highest to the lowest position of the piston
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
            drawer.rectangle(Rectangle.fromCenter(Vector2(axle.x, axle.y - rod), 80.0, 2 * crank + 70.0))
            drawer.circle(axle, crank + 20.0)
            // the piston, the rod and the crank
            drawer.fill = ColorRGBa.GRAY
            drawer.stroke = null
            drawer.rectangle(Rectangle.fromCenter(piston, 74.0, 50.0))
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 6.0
            drawer.lineSegment(pin, piston)
            drawer.stroke = ColorRGBa.PINK
            drawer.lineSegment(axle, pin)
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer)
        }
    }
}
