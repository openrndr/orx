import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.gradientdescent.minimize
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A four-bar linkage, of two parameters, the angles of the crank and of the coupler: that the coupler reaches the
 * rocker is not built in, it is a penalty that is minimized with everything else, see [DragParameters.penalty], so
 * the same gradient descent that follows the mouse keeps the linkage closed. The crank turns on its own while nothing
 * is dragged; dragging the crank pin, the end of the coupler or the point on the coupler turns the linkage to the
 * mouse, as far as it can go. The point on the coupler draws the coupler curve.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val crankPivot = Vector2(-130.0, 80.0)
        val rockerPivot = Vector2(110.0, 80.0)
        val crank = 60.0
        val coupler = 200.0
        val rocker = 150.0

        fun pin(x: DoubleArray) = crankPivot + Vector2(cos(x[0]), sin(x[0])) * crank
        fun end(x: DoubleArray) = pin(x) + Vector2(cos(x[1]), sin(x[1])) * coupler

        /** the point on the coupler, above the middle of it */
        fun tracer(x: DoubleArray): Vector2 {
            val b = pin(x)
            val d = end(x) - b
            return b + d * 0.5 + d.perpendicular() * 0.7
        }

        /** how far the coupler misses the rocker, squared and weighted */
        fun penalty(x: DoubleArray): Double {
            val miss = end(x).distanceTo(rockerPivot) - rocker
            return 1000.0 * miss * miss
        }

        /** the parameters with the coupler turned such that the linkage closes, from where it was */
        fun closed(x: DoubleArray): DoubleArray {
            val angle = minimize(doubleArrayOf(x[1])) { (phi) -> penalty(doubleArrayOf(x[0], phi)) }.solution[0]
            return doubleArrayOf(x[0], angle)
        }

        // the coupler curve, the crank once around, every position from the one before
        var traced = closed(doubleArrayOf(0.0, -0.6))
        val curve = (0..360).map {
            traced = closed(doubleArrayOf(it * 2 * PI / 360, traced[1]))
            tracer(traced)
        }

        val drag = extend(DragParameters(closed(doubleArrayOf(0.0, -0.6)), drawer.bounds.center) { x ->
            listOf(pin(x), end(x), tracer(x))
        })
        drag.penalty = ::penalty
        drag.damping = 100.0

        var last = seconds
        extend {
            if (drag.dragged < 0) {
                closed(doubleArrayOf(drag.parameters[0] + (seconds - last) * 1.2, drag.parameters[1])).copyInto(drag.parameters)
            }
            last = seconds
            drawer.translate(drag.origin)
            val x = drag.parameters
            val pin = pin(x)
            val end = end(x)
            val tracer = tracer(x)

            drawer.stroke = ColorRGBa.PINK.opacify(0.5)
            drawer.lineLoop(curve)
            drawer.lineCap = LineCap.ROUND
            drawer.strokeWeight = 8.0
            drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
            drawer.lineSegment(crankPivot, rockerPivot)
            drawer.stroke = ColorRGBa.PINK
            drawer.lineSegment(crankPivot, pin)
            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rockerPivot, end)
            // the coupler, a triangle with the point on it
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff")
            drawer.lineStrip(listOf(pin, tracer, end, pin))
            drawer.strokeWeight = 1.0
            drawer.fill = ColorRGBa.GRAY
            drawer.stroke = null
            drawer.circles(listOf(crankPivot, rockerPivot), 10.0)
            drag.drawPoints(drawer)
        }
    }
}
