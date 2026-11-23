import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

/**
 * The circle through three points, of which the center is the crossing of the perpendicular bisectors of two chords.
 * The three points and the center can be dragged, see [DragParameters]: dragging the center moves the three points,
 * as little as gradient descent finds, to put it where the mouse is.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        /** the center of the circle through [a], [b] and [c], not finite when they are on a line */
        fun center(a: Vector2, b: Vector2, c: Vector2): Vector2 {
            val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
            val a2 = a.squaredLength
            val b2 = b.squaredLength
            val c2 = c.squaredLength
            return Vector2(
                (a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d,
                (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d
            )
        }

        val drag = extend(DragParameters(doubleArrayOf(-200.0, 100.0, 0.0, -150.0, 180.0, 60.0), drawer.bounds.center) { x ->
            val a = Vector2(x[0], x[1])
            val b = Vector2(x[2], x[3])
            val c = Vector2(x[4], x[5])
            listOf(a, b, c, center(a, b, c))
        })
        extend {
            drawer.translate(drag.origin)
            val (a, b, c, m) = drag.points(drag.parameters)
            drawer.fill = null
            if (m.x.isFinite() && m.y.isFinite()) {
                // the perpendicular bisectors of the chords meet in the center
                for ((p, q) in listOf(a to b, c to b)) {
                    val mid = (p + q) / 2.0
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
                    drawer.lineSegment(mid, m)
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                    drawer.lineSegment(p, q)
                }
                drawer.stroke = ColorRGBa.PINK.opacify(0.8)
                drawer.circle(m, m.distanceTo(a))
            }
            drag.drawPoints(drawer)
        }
    }
}
