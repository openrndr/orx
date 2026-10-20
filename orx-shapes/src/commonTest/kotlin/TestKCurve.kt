import org.openrndr.extra.shapes.kcurve.kCurve
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestKCurve {

    private fun cross(a: Vector2, b: Vector2) = a.x * b.y - a.y * b.x

    private fun curvature(segment: Segment2D, t: Double): Double {
        val d1 = segment.derivative(t)
        val d2 = segment.derivative2(t)
        return cross(d1, d2) / (d1.length * d1.length * d1.length)
    }

    /** The parameter of maximum curvature of a quadratic, where its derivative is perpendicular to (c0 - 2c1 + c2). */
    private fun peak(segment: Segment2D): Double {
        val c0 = segment.start
        val c1 = segment.control[0]
        val c2 = segment.end
        val d = c0 - c1 * 2.0 + c2
        return -(c1 - c0).dot(d) / d.squaredLength
    }

    /** The distance from [p] to the quadratic [segment], by sampling refined with ternary search. */
    private fun distance(segment: Segment2D, p: Vector2): Double {
        fun d(t: Double) = segment.position(t).distanceTo(p)
        val k = (0..200).minBy { d(it / 200.0) }
        var lo = maxOf(0.0, (k - 1) / 200.0)
        var hi = minOf(1.0, (k + 1) / 200.0)
        repeat(100) {
            val m1 = lo + (hi - lo) / 3.0
            val m2 = hi - (hi - lo) / 3.0
            if (d(m1) < d(m2)) hi = m2 else lo = m1
        }
        return d((lo + hi) / 2.0)
    }

    /** Asserts what holds for every κ-curve: one quadratic per point, through that point, and G1. */
    private fun assertInterpolatingG1(contour: ShapeContour, targets: List<Vector2>, message: String) {
        val s = contour.segments
        assertEquals(targets.size, s.size, "$message: one quadratic per point")
        val scale = targets.zipWithNext { a, b -> a.distanceTo(b) }.maxOrNull() ?: 1.0
        for ((i, segment) in s.withIndex()) {
            assertEquals(1, segment.control.size, "$message: quadratic")
            assertTrue(distance(segment, targets[i]) < 1E-8 * scale, "$message: quadratic $i misses its point")
        }
        val joints = if (contour.closed) s.size else s.size - 1
        for (i in 0 until joints) {
            val a = s[i]
            val b = s[(i + 1) % s.size]
            assertTrue(a.end.distanceTo(b.start) < 1E-9 * scale, "$message: joint $i doesn't connect")
            assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-9, "$message: tangent jumps at $i")
        }
    }

    /** Asserts every κ-curve property, with [targets] the points that must be curvature maxima. */
    private fun assertKCurve(contour: ShapeContour, targets: List<Vector2>, message: String) {
        assertInterpolatingG1(contour, targets, message)
        val s = contour.segments
        val scale = targets.zipWithNext { a, b -> a.distanceTo(b) }.maxOrNull() ?: 1.0
        for ((i, segment) in s.withIndex()) {
            assertEquals(1, segment.control.size, "$message: quadratic")
            val t = peak(segment)
            assertTrue(t > 0.0 && t < 1.0, "$message: peak of quadratic $i at $t")
            assertTrue(segment.position(t).distanceTo(targets[i]) < 1E-8 * scale, "$message: point $i isn't the peak")
            // nothing on the quadratic bends more sharply than at the point
            val peakCurvature = abs(curvature(segment, t))
            for (k in 0..20) assertTrue(abs(curvature(segment, k / 20.0)) <= peakCurvature * (1 + 1E-9))
        }
        val joints = if (contour.closed) s.size else s.size - 1
        for (i in 0 until joints) {
            val a = s[i]
            val b = s[(i + 1) % s.size]
            val ka = curvature(a, 1.0)
            val kb = curvature(b, 0.0)
            // G2 except at inflections, where the quadratics turn opposite ways
            if (cross(a.control[0] - a.start, a.end - a.control[0]) * cross(b.control[0] - b.start, b.end - b.control[0]) > 0.0) {
                assertEquals(ka, kb, 1E-6 * maxOf(abs(ka), abs(kb)), "$message: curvature jumps at $i")
            }
        }
    }

    @Test
    fun regularPolygon() {
        val points = List(6) { Vector2(cos(it * PI / 3), sin(it * PI / 3)) * 100.0 }
        val contour = points.kCurve(closed = true)
        assertTrue(contour.closed)
        assertKCurve(contour, points, "hexagon")
        // symmetry: every point has the same curvature
        val peaks = contour.segments.map { abs(curvature(it, peak(it))) }
        for (k in peaks) assertEquals(peaks[0], k, 1E-9 * peaks[0])
    }

    @Test
    fun open() {
        val points = listOf(Vector2(0.0, 0.0), Vector2(60.0, 80.0), Vector2(140.0, 20.0), Vector2(220.0, 90.0), Vector2(300.0, 0.0))
        val contour = points.kCurve(closed = false)
        assertTrue(contour.segments.first().start.distanceTo(points.first()) < 1E-12)
        assertTrue(contour.segments.last().end.distanceTo(points.last()) < 1E-12)
        assertKCurve(contour, points.subList(1, points.size - 1), "open")
    }

    @Test
    fun threePoints() {
        // a single quadratic with its curvature peak at the middle point
        val points = listOf(Vector2(0.0, 0.0), Vector2(50.0, 100.0), Vector2(200.0, 0.0))
        val contour = points.kCurve(closed = false)
        assertKCurve(contour, points.subList(1, 2), "three points")
    }

    @Test
    fun randomPoints() {
        val random = Random(1)
        var complete = 0
        repeat(100) {
            val points = List(3 + it % 8) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
            val closed = it % 2 == 1
            val contour = points.kCurve(closed)
            val targets = if (closed) points else points.subList(1, points.size - 1)
            // always interpolating and G1, even where no curve has its curvature maxima at all points
            assertInterpolatingG1(contour, targets, "random $it")
            if (runCatching { assertKCurve(contour, targets, "random $it") }.isSuccess) complete++
        }
        // 90 at the time of writing; the others are erratic point sets that have no κ-curve
        assertTrue(complete >= 85, "only $complete of 100 random κ-curves have all their properties")
    }
}
