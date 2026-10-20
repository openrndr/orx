import org.openrndr.extra.shapes.splines.CircleSplineSegment
import org.openrndr.extra.shapes.splines.circleSpline
import org.openrndr.extra.shapes.splines.circleSplineSegments
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCircleSpline {

    private fun cross(a: Vector2, b: Vector2) = a.x * b.y - a.y * b.x

    /**
     * The signed curvature of [segment] at [t], from central differences of its exact position; the blended arcs
     * extend smoothly beyond [0, 1], so the stencil can be centered at the ends too.
     */
    private fun curvature(segment: CircleSplineSegment, t: Double): Double {
        fun derivatives(h: Double): Pair<Vector2, Vector2> {
            val p0 = segment.position(t - h)
            val p1 = segment.position(t)
            val p2 = segment.position(t + h)
            return Pair((p2 - p0) / (2 * h), (p2 - p1 * 2.0 + p0) / (h * h))
        }
        // Richardson extrapolation of the central differences cancels their h² error
        val h = 1E-3
        val (a1, a2) = derivatives(h)
        val (b1, b2) = derivatives(h / 2)
        val d1 = (b1 * 4.0 - a1) / 3.0
        val d2 = (b2 * 4.0 - a2) / 3.0
        return cross(d1, d2) / (d1.length * d1.length * d1.length)
    }

    /** The distance from [q] to [segment], by dense sampling refined with ternary search. */
    private fun distance(segment: CircleSplineSegment, q: Vector2): Double {
        fun d(t: Double) = segment.position(t).distanceTo(q)
        val k = (0..400).minBy { d(it / 400.0) }
        var lo = maxOf(0, k - 1) / 400.0
        var hi = minOf(400, k + 1) / 400.0
        repeat(100) {
            val m1 = lo + (hi - lo) / 3.0
            val m2 = hi - (hi - lo) / 3.0
            if (d(m1) < d(m2)) hi = m2 else lo = m1
        }
        return d((lo + hi) / 2.0)
    }

    /** The signed curvature of the circle through [x], [a] and [b], traversed in that order. */
    private fun circleCurvature(x: Vector2, a: Vector2, b: Vector2): Double {
        val area2 = cross(a - x, b - a)
        return 2.0 * area2 / ((a - x).length * (b - a).length * (b - x).length)
    }

    @Test
    fun pointsOnACircle() {
        val random = Random(1)
        val center = Vector2(200.0, 150.0)
        for (sign in listOf(1.0, -1.0)) {
            val angles = List(7) { random.nextDouble(0.0, 2 * PI) }.sorted().map { it * sign }
            val points = angles.map { center + Vector2(cos(it), sin(it)) * 100.0 }
            for (closed in listOf(false, true)) {
                for (segment in points.circleSplineSegments(closed)) for (k in 0..20) {
                    assertEquals(100.0, segment.position(k / 20.0).distanceTo(center), 1E-9, "sign $sign closed $closed")
                }
            }
        }
    }

    @Test
    fun collinear() {
        val points = List(5) { Vector2(it * 40.0, it * 15.0) }
        for (segment in points.circleSplineSegments(false)) for (k in 0..10) {
            val p = segment.position(k / 10.0)
            assertEquals(0.0, p.x * 15.0 - p.y * 40.0, 1E-9)
        }
    }

    @Test
    fun g2WithTheCurvatureOfTheCircles() {
        val random = Random(2)
        repeat(30) {
            val points = List(4 + it % 5) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
            val segments = points.circleSplineSegments(closed = true)
            val n = points.size
            for (i in 0 until n) {
                val incoming = segments[(i - 1 + n) % n]
                val outgoing = segments[i]
                // the same tangent on both sides of every point
                assertTrue(incoming.derivative(1.0).normalized.distanceTo(outgoing.derivative(0.0).normalized) < 1E-5)
                // and the curvature of the circle through the point and its neighbours
                val expected = circleCurvature(points[(i - 1 + n) % n], points[i], points[(i + 1) % n])
                val k1 = curvature(incoming, 1.0)
                val k2 = curvature(outgoing, 0.0)
                assertEquals(expected, k1, 1E-5 * (abs(expected) + 1E-3), "incoming curvature at $i")
                assertEquals(expected, k2, 1E-5 * (abs(expected) + 1E-3), "outgoing curvature at $i")
            }
        }
    }

    @Test
    fun contour() {
        val random = Random(3)
        val points = List(8) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
        for (closed in listOf(false, true)) {
            val contour = points.circleSpline(closed, tolerance = 0.01)
            assertEquals(closed, contour.closed)
            val joints = contour.segments.map { it.start } + contour.segments.last().end
            for (p in points) assertTrue(joints.any { it.distanceTo(p) < 1E-9 })
            // the Béziers stay within the tolerance of the exact spline
            val segments = points.circleSplineSegments(closed)
            for (k in 0..200) {
                val q = contour.position(k / 200.0)
                val error = segments.minOf { distance(it, q) }
                assertTrue(error < 0.011, "the contour deviates by $error")
            }
        }
        // two points: a straight line
        assertTrue(listOf(Vector2.ZERO, Vector2(10.0, 0.0)).circleSpline(false).segments.all { s -> s.control.all { abs(it.y) < 1E-9 } })
    }
}
