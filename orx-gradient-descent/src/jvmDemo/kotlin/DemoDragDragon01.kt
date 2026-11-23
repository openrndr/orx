import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2

/**
 * A dragon curve between two end points: every segment is folded at its middle, which moves sideways by the
 * squareness times half its length, to the left and the right in turn, nine times over. Dragging any corner of the
 * curve changes the squareness and the end points, see [DragParameters].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

        /** the corners of the curve from [a] to [b], without [b] */
        fun dragon(a: Vector2, b: Vector2, depth: Int, squareness: Double, side: Double, corners: MutableList<Vector2>) {
            if (depth == 0) {
                corners.add(a)
                return
            }
            val d = b - a
            val middle = (a + b) / 2.0 + d.perpendicular() * (side * squareness / 2.0)
            dragon(a, middle, depth - 1, squareness, 1.0, corners)
            dragon(middle, b, depth - 1, squareness, -1.0, corners)
        }

        val drag = extend(DragParameters(doubleArrayOf(0.8, -180.0, 60.0, 200.0, 60.0), drawer.bounds.center) { x ->
            val from = Vector2(x[1], x[2])
            val to = Vector2(x[3], x[4])
            mutableListOf<Vector2>().also { dragon(from, to, 9, x[0], 1.0, it); it.add(to) }
        })
        extend {
            drawer.fontMap = font
            drawer.translate(drag.origin)
            val corners = drag.points(drag.parameters)
            drawer.stroke = ColorRGBa.WHITE
            drawer.lineStrip(corners)
            // the end points, the rest of the corners can be dragged too
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.circles(listOf(corners.first(), corners.last()), 6.0)
            if (drag.hovered >= 0 || drag.dragged >= 0) {
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(corners[if (drag.dragged >= 0) drag.dragged else drag.hovered], 6.0)
            }
            drawer.fill = ColorRGBa.WHITE
            drawer.text("squareness = ${"%.3f".format(drag.parameters[0])}", -340.0, 340.0)
        }
    }
}
