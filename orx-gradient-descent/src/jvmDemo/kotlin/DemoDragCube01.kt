import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * A cube in perspective of two angles of rotation, around the x and the y axis. Dragging a corner turns the cube,
 * see [DragParameters]: the angles change such that the corner goes where the mouse is, as far as turning can bring it.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        // the corners at ±0.5, of which the bits of the index tell the coordinates
        val corners = (0 until 8).map { Vector3((it and 1) - 0.5, ((it shr 1) and 1) - 0.5, ((it shr 2) and 1) - 0.5) }
        // the edges join the corners that differ in one coordinate
        val edges = (0 until 8).flatMap { i -> listOf(1, 2, 4).filter { i and it == 0 }.map { i to (i or it) } }

        fun project(corner: Vector3, ax: Double, ay: Double): Vector2 {
            val y = corner.y * cos(ax) - corner.z * sin(ax)
            val z0 = corner.y * sin(ax) + corner.z * cos(ax)
            val x = corner.x * cos(ay) + z0 * sin(ay)
            val z = -corner.x * sin(ay) + z0 * cos(ay) + 2.5
            return Vector2(x, y) * (600.0 / z)
        }

        val drag = extend(DragParameters(doubleArrayOf(0.5, 0.6), drawer.bounds.center) { (ax, ay) ->
            corners.map { project(it, ax, ay) }
        })
        extend {
            drawer.translate(drag.origin)
            val points = drag.points(drag.parameters)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.6)
            drawer.strokeWeight = 4.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegments(edges.flatMap { (i, j) -> listOf(points[i], points[j]) })
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer)
        }
    }
}
