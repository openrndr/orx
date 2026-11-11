package offset

import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.extra.shapes.primitives.regularStarRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import org.openrndr.shape.contains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class TestOffsetRegion {
    private val center = Vector2(320.0, 240.0)
    private val shapes = listOf(
        "plain star" to regularStar(5, 60.0, 150.0, center).shape,
        "rounded star" to regularStarRounded(5, 60.0, 150.0, 0.5, 0.5, center).shape,
        "blob" to hobbyCurve(List(9) { k ->
            val a = k * 2 * PI / 9
            center + Vector2(cos(a), sin(a)) * (110.0 + 40.0 * sin(3 * a))
        }, true).shape,
        "rectangle" to Rectangle.fromCenter(center, 260.0, 160.0).shape,
        "ring" to Shape(listOf(Circle(center, 140.0).contour.clockwise, Circle(center, 60.0).contour.counterClockwise)),
    )

    /**
     * Compares offsets with the true offset region on a grid: a point is in it when its signed distance to the shape's
     * outline, negative inside, is below the distance. Points within two grid steps of the region's boundary are not
     * judged.
     */
    @Test
    fun offsetsAreTheRegionAtTheirDistance() {
        val n = 60
        for ((name, shape) in shapes) {
            val b = shape.bounds.offsetEdges(110.0)
            val step = maxOf(b.width, b.height) / n
            val grid = List(n * n) { k -> Vector2(b.x + (k % n + 0.5) * step, b.y + (k / n + 0.5) * step) }
            val signed = grid.map { p ->
                val d = shape.contours.minOf { it.nearest(p).position.distanceTo(p) }
                if (p in shape) -d else d
            }
            for (distance in listOf(-70.0, -64.0, -45.0, -20.0, -5.0, 5.0, 30.0, 90.0)) {
                val result = shape.offset(distance, SegmentJoin.ROUND)
                var wrong = 0
                for ((k, p) in grid.withIndex()) {
                    val margin = signed[k] - distance
                    if (abs(margin) < 2 * step) continue
                    if ((p in result) != (margin < 0.0)) wrong++
                }
                assertTrue(wrong == 0, "$name at $distance: $wrong points on the wrong side")
                // the outlines of the result are at the distance from the outlines of the shape
                for (c in result.contours) for (p in c.equidistantPositions(20)) {
                    val d = shape.contours.minOf { it.nearest(p).position.distanceTo(p) }
                    assertTrue(abs(d - abs(distance)) < 0.5, "$name at $distance: a point of the result is $d from the shape")
                }
            }
        }
    }
}
