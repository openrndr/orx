import org.openrndr.extra.shapes.metro.metro
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.intersection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMetro {

    private fun assertDirections(points: List<Vector2>, closed: Boolean, directions: Int) {
        val n = if (closed) points.size else points.size - 1
        for (i in 0 until n) {
            val d = points[(i + 1) % points.size] - points[i]
            if (d.length < 1E-9) continue
            val steps = atan2(d.y, d.x) / (PI / directions)
            assertTrue(abs(steps - round(steps)) < 1E-6, "segment $i has direction ${d.normalized}")
        }
    }

    @Test
    fun rectangleIsKept() {
        val rectangle = Rectangle(10.0, 20.0, 200.0, 100.0)
        val contour = rectangle.contour.metro(directions = 2)
        assertTrue(contour.closed)
        assertEquals(4, contour.segments.size)
        for (corner in listOf(rectangle.corner, rectangle.corner + Vector2(200.0, 100.0))) {
            assertTrue(contour.segments.any { it.start.distanceTo(corner) < 1E-6 }, "corner $corner")
        }
    }

    @Test
    fun circleIsOctilinear() {
        val contour = Circle(0.0, 0.0, 100.0).contour.metro(directions = 4)
        val points = contour.segments.map { it.start }
        assertDirections(points, true, 4)
        assertEquals(8, points.size)
        for (p in points) assertTrue(abs(p.length - 100.0) < 15.0, "vertex $p near the circle")
    }

    @Test
    fun noisyOpenPath() {
        val points = List(100) { Vector2(it * 4.0, 20.0 * sin(it * 0.3) + if (it > 50) (it - 50) * 4.0 else 0.0) }
        for (directions in 2..6) {
            val simple = points.metro(closed = false, directions = directions)
            assertDirections(simple, false, directions)
            val simpler = points.metro(closed = false, directions = directions, segmentPenalty = 1E4, boundsWeight = 1.0)
            assertDirections(simpler, false, directions)
            assertTrue(simpler.size <= simple.size, "$directions: ${simpler.size} <= ${simple.size}")
        }
    }

    @Test
    fun straightLine() {
        val points = List(10) { Vector2(it * 10.0, it * 10.0) }
        val result = points.metro(closed = false, directions = 4)
        assertEquals(2, result.size)
        assertTrue(result[0].distanceTo(points.first()) < 1E-6, "start ${result[0]}")
        assertTrue(result[1].distanceTo(points.last()) < 1E-6, "end ${result[1]}")
    }

    /**
     * A short block whose neighbours' lines cross beyond it gets a reversed segment, the output then overshoots and
     * doubles back, which shows up as segment i crossing segment i + 2. Random walks that cross themselves are skipped,
     * as their loops are legitimately reproduced.
     */
    @Test
    fun noReversedSegmentLoops() {
        val random = Random(1)
        var tested = 0
        for (trial in 0 until 100) {
            var position = Vector2.ZERO
            var heading = 0.0
            val points = List(200) {
                heading = heading * 0.95 + random.nextDouble(-0.6, 0.6)
                position += Vector2(cos(heading), sin(heading)) * random.nextDouble(2.0, 8.0)
                position
            }
            val curve = ShapeContour.fromPoints(points, closed = false)
            val linear = curve.segments
            val selfIntersecting = linear.indices.any { i ->
                (i + 2 until linear.size).any { j ->
                    intersection(linear[i].start, linear[i].end, linear[j].start, linear[j].end) != Vector2.INFINITY
                }
            }
            if (selfIntersecting) continue
            tested++
            for (directions in 2..6) for (penalty in listOf(0.0, 100.0)) {
                val s = curve.metro(directions = directions, segmentPenalty = penalty, boundsWeight = penalty / 100.0).segments
                for (i in 0 until s.size - 2) {
                    val p = intersection(s[i].start, s[i].end, s[i + 2].start, s[i + 2].end)
                    assertTrue(p == Vector2.INFINITY, "trial $trial, $directions directions, penalty $penalty: segments $i and ${i + 2} cross at $p")
                }
            }
        }
        assertTrue(tested >= 20, "only $tested paths without self intersections")
    }
}
