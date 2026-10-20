import org.openrndr.extra.shapes.splines.BSpline
import org.openrndr.extra.shapes.splines.bSpline
import org.openrndr.extra.shapes.splines.interpolatingBSpline
import org.openrndr.extra.shapes.splines.toContour
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBSpline {

    private val random = Random(1)
    private fun randomPoints(n: Int) = List(n) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }

    private fun bernstein(points: List<Vector2>, t: Double): Vector2 {
        val p = points.toMutableList()
        for (r in 1 until p.size) for (i in 0 until p.size - r) p[i] = p[i] * (1.0 - t) + p[i + 1] * t
        return p[0]
    }

    @Test
    fun clampedEnds() {
        for (degree in 1..5) {
            val points = randomPoints(degree + 4)
            val spline = BSpline.clamped(points, degree)
            assertTrue(spline.position(spline.domain.start).distanceTo(points.first()) < 1E-9)
            assertTrue(spline.position(spline.domain.endInclusive).distanceTo(points.last()) < 1E-9)
            // the curve leaves and arrives along the first and last edge of the control polygon
            assertTrue(spline.derivative(spline.domain.start).normalized.distanceTo((points[1] - points[0]).normalized) < 1E-9)
            assertTrue(spline.derivative(spline.domain.endInclusive).normalized.distanceTo((points.last() - points[points.size - 2]).normalized) < 1E-9)
        }
    }

    @Test
    fun singleSpanIsBezier() {
        for (degree in 1..5) {
            val points = randomPoints(degree + 1)
            val spline = BSpline.clamped(points, degree)
            for (k in 0..10) assertTrue(spline.position(k / 10.0).distanceTo(bernstein(points, k / 10.0)) < 1E-9)
        }
    }

    @Test
    fun knotInsertionAndBezierPieces() {
        for (degree in 1..5) for (closed in listOf(false, true)) {
            val spline = randomPoints(degree + 5).bSpline(degree, closed)
            // a parameter that is not a knot
            val inserted = spline.insertKnot(spline.domain.start + 0.37 * (spline.domain.endInclusive - spline.domain.start))
            val breaks = spline.knots.filter { it in spline.domain }.distinct()
            val pieces = spline.bezierPieces()
            assertEquals(breaks.size - 1, pieces.size)
            for ((piece, span) in pieces.zip(breaks.zipWithNext())) {
                for (k in 0..10) {
                    val t = k / 10.0
                    val u = span.first + t * (span.second - span.first)
                    val expected = spline.position(u)
                    assertTrue(inserted.position(u).distanceTo(expected) < 1E-9, "insertion changes the curve, degree $degree")
                    assertTrue(bernstein(piece, t).distanceTo(expected) < 1E-9, "piece differs, degree $degree closed $closed")
                }
            }
        }
    }

    @Test
    fun continuity() {
        // a cubic B-spline with single knots is C2: the first and second derivatives are continuous at the knots
        val spline = randomPoints(9).bSpline(3, closed = false)
        val first = spline.derivativeSpline()!!
        val second = first.derivativeSpline()!!
        val h = 1E-9
        for (u in spline.knots.filter { it > spline.domain.start && it < spline.domain.endInclusive }.distinct()) {
            val d1 = first.position(u)
            val d2 = second.position(u)
            assertTrue(first.position(u - h).distanceTo(first.position(u + h)) < 1E-6 * (1.0 + d1.length))
            assertTrue(second.position(u - h).distanceTo(second.position(u + h)) < 1E-6 * (1.0 + d2.length))
        }
        // the derivative matches finite differences
        for (k in 1..9) {
            val u = k / 10.0
            val numeric = (spline.position(u + 1E-6) - spline.position(u - 1E-6)) / 2E-6
            assertTrue(numeric.distanceTo(spline.derivative(u)) < 1E-4 * (1.0 + numeric.length))
        }
    }

    @Test
    fun periodic() {
        val points = randomPoints(7)
        val spline = points.bSpline(3, closed = true)
        assertTrue(spline.closed)
        val (a, b) = spline.domain.start to spline.domain.endInclusive
        assertTrue(spline.position(a).distanceTo(spline.position(b)) < 1E-9)
        assertTrue(spline.derivative(a).distanceTo(spline.derivative(b)) < 1E-9)
        // a uniform cubic B-spline passes through (Pᵢ₋₁ + 4Pᵢ + Pᵢ₊₁) / 6 at its knots
        for (i in 0 until points.size) {
            val expected = (points[i] + points[(i + 1) % 7] * 4.0 + points[(i + 2) % 7]) / 6.0
            assertTrue(spline.position(a + i).distanceTo(expected) < 1E-9)
        }
        val contour = spline.toContour()
        assertTrue(contour.closed)
        assertEquals(7, contour.segments.size)
    }

    /** The parameters at which an open interpolating spline passes through its points: the start, end and between. */
    private fun assertPassesThrough(spline: BSpline<Vector2>, points: List<Vector2>, message: String) {
        val samples = (0..4000).map { spline.position(spline.domain.start + it / 4000.0 * (spline.domain.endInclusive - spline.domain.start)) }
        for (p in points) {
            // refine the nearest sample with ternary search on the parameter
            val k = samples.indices.minBy { samples[it].distanceTo(p) }
            val span = spline.domain.endInclusive - spline.domain.start
            var lo = spline.domain.start + maxOf(0, k - 1) / 4000.0 * span
            var hi = spline.domain.start + minOf(4000, k + 1) / 4000.0 * span
            repeat(100) {
                val m1 = lo + (hi - lo) / 3.0
                val m2 = hi - (hi - lo) / 3.0
                if (spline.position(m1).distanceTo(p) < spline.position(m2).distanceTo(p)) hi = m2 else lo = m1
            }
            assertTrue(spline.position((lo + hi) / 2.0).distanceTo(p) < 1E-7, "$message: misses $p")
        }
    }

    @Test
    fun interpolation() {
        for (degree in 1..5) for (alpha in listOf(0.0, 0.5, 1.0)) {
            val points = randomPoints(8)
            val spline = points.interpolatingBSpline(degree, closed = false, alpha = alpha)
            assertEquals(degree, spline.degree)
            assertTrue(spline.position(spline.domain.start).distanceTo(points.first()) < 1E-9)
            assertTrue(spline.position(spline.domain.endInclusive).distanceTo(points.last()) < 1E-9)
            assertPassesThrough(spline, points, "degree $degree, alpha $alpha")
        }
        for (degree in 1..5) for (n in listOf(6, 7)) {
            val points = randomPoints(n)
            val spline = points.interpolatingBSpline(degree, closed = true)
            assertTrue(spline.closed)
            val offset = if (degree % 2 == 1) 0.0 else 0.5
            for ((i, p) in points.withIndex()) {
                assertTrue(spline.position(spline.domain.start + i + offset).distanceTo(p) < 1E-9, "closed degree $degree, $n points")
            }
        }
        // collinear points give a straight line, few points a lower degree
        val line = List(5) { Vector2(it * 30.0, it * 10.0) }.interpolatingBSpline(3)
        for (k in 0..20) {
            val p = line.position(k / 20.0)
            assertEquals(0.0, p.x * 10.0 - p.y * 30.0, 1E-9)
        }
        assertEquals(2, randomPoints(3).interpolatingBSpline(3).degree)
    }

    /** The distance from [q] to [spline], by dense sampling refined with ternary search on the parameter. */
    private fun distance(spline: BSpline<Vector2>, q: Vector2): Double {
        val n = 2000
        fun d(u: Double) = spline.position(u).distanceTo(q)
        val k = (0..n).minBy { d(it.toDouble() / n) }
        var lo = maxOf(0, k - 1).toDouble() / n
        var hi = minOf(n, k + 1).toDouble() / n
        repeat(100) {
            val m1 = lo + (hi - lo) / 3.0
            val m2 = hi - (hi - lo) / 3.0
            if (d(m1) < d(m2)) hi = m2 else lo = m1
        }
        return d((lo + hi) / 2.0)
    }

    @Test
    fun contours() {
        for (degree in 1..5) {
            val spline = randomPoints(8).bSpline(degree)
            val contour = spline.toContour(tolerance = 0.01)
            // exact up to degree 3, within the tolerance above
            for (k in 0..200) {
                val error = distance(spline, contour.position(k / 200.0))
                assertTrue(error < if (degree <= 3) 1E-9 else 0.01, "degree $degree deviates by $error")
            }
            if (degree <= 3) assertEquals(spline.bezierPieces().size, contour.segments.size)
        }
        val path = List(6) { Vector3(random.nextDouble(), random.nextDouble(), random.nextDouble()) }.bSpline(3, closed = true).toPath3D()
        assertTrue(path.closed)
        assertEquals(6, path.segments.size)
    }
}
