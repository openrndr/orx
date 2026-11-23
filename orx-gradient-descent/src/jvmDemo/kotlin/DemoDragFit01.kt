import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.gradientdescent.minimize
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Fitting: a circle fitted to points by least squares, the sum of the squared distances of the points to the circle
 * minimized with [minimize] every frame, starting from the circle of the frame before. The points can be dragged, see
 * [DragParameters], and the circle follows; the lines show how far every point is from it.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

        // twelve points around a circle, not quite on it
        val random = Random(1)
        val count = 12
        val start = (0 until count).flatMap { i ->
            val angle = i * 2 * PI / count + random.nextDouble(-0.2, 0.2)
            val r = 200.0 + random.nextDouble(-30.0, 30.0)
            listOf(r * cos(angle), r * sin(angle))
        }.toDoubleArray()

        val drag = extend(DragParameters(start, drawer.bounds.center) { x ->
            (0 until count).map { Vector2(x[2 * it], x[2 * it + 1]) }
        })
        // a point is moved by its own coordinates only
        drag.affects = { intArrayOf(2 * it, 2 * it + 1) }

        // the center and the radius of the circle
        var circle = doubleArrayOf(0.0, 0.0, 200.0)
        extend {
            val points = drag.points(drag.parameters)
            // when the fit fails, like for points that all coincide, the circle stays
            circle = runCatching {
                minimize(circle) { (x, y, r) ->
                    points.sumOf { val d = it.distanceTo(Vector2(x, y)) - r; d * d }
                }.solution
            }.getOrDefault(circle)
            val (x, y, r) = circle
            val center = Vector2(x, y)

            drawer.translate(drag.origin)
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.strokeWeight = 2.0
            drawer.circle(center, r)
            drawer.strokeWeight = 1.0
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            drawer.lineSegments(points.flatMap { listOf(it, center + (it - center).normalized * r) })
            drawer.lineSegment(center - Vector2(6.0, 0.0), center + Vector2(6.0, 0.0))
            drawer.lineSegment(center - Vector2(0.0, 6.0), center + Vector2(0.0, 6.0))
            drag.drawPoints(drawer)

            val rms = sqrt(points.sumOf { val d = it.distanceTo(center) - r; d * d } / count)
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("radius ${"%.1f".format(r)}, rms distance ${"%.2f".format(rms)}", -340.0, 340.0)
        }
    }
}
