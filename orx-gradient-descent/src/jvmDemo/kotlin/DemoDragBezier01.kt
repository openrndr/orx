import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import kotlin.math.max
import kotlin.math.min

/**
 * A quadratic Bézier curve of three control points and the point at t on it, with the construction of de Casteljau
 * that finds that point. All four points can be dragged, see [DragParameters]: dragging the point on the curve moves
 * the control points and t together, so the point can slide along the curve and pull it.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

        fun at(p0: Vector2, p1: Vector2, p2: Vector2, t: Double): Vector2 = p0.mix(p1, t).mix(p1.mix(p2, t), t)

        val drag = extend(DragParameters(doubleArrayOf(-250.0, 150.0, 0.0, -250.0, 250.0, 150.0, 0.4), drawer.bounds.center) { x ->
            val p0 = Vector2(x[0], x[1])
            val p1 = Vector2(x[2], x[3])
            val p2 = Vector2(x[4], x[5])
            listOf(p0, p1, p2, at(p0, p1, p2, x[6]))
        })
        extend {
            drawer.fontMap = font
            drawer.translate(drag.origin)
            val (p0, p1, p2) = drag.points(drag.parameters)
            val t = drag.parameters[6]

            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.lineStrip(listOf(p0, p1, p2))
            // the curve, also past its ends when t is outside [0, 1]
            val from = min(0.0, t)
            val to = max(1.0, t)
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.lineStrip((0..100).map { at(p0, p1, p2, from + (to - from) * it / 100.0) })
            drawer.strokeWeight = 1.0
            // the construction: the line between the points at t on both legs touches the curve at t
            drawer.stroke = ColorRGBa.PINK.opacify(0.6)
            drawer.lineSegment(p0.mix(p1, t), p1.mix(p2, t))
            drag.drawPoints(drawer)
            drawer.fill = ColorRGBa.WHITE
            drawer.text("t = ${"%.3f".format(t)}", -340.0, 340.0)
        }
    }
}
