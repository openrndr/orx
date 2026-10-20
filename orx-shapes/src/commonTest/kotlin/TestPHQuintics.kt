import org.openrndr.extra.shapes.ph.PHQuintic
import org.openrndr.extra.shapes.ph.phQuinticHermite
import org.openrndr.extra.shapes.ph.phQuinticSpline
import org.openrndr.extra.shapes.ph.phQuintics
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPHQuintics {

    private fun randomPoint(random: Random) = Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0))
    private fun randomVector(random: Random) = Vector2(random.nextDouble(-300.0, 300.0), random.nextDouble(-300.0, 300.0))

    private fun randomHermite(random: Random): PHQuintic =
        phQuinticHermite(randomPoint(random), randomVector(random), randomPoint(random), randomVector(random))

    /** The derivative of the quintic Bézier with the control points of [curve]. */
    private fun bezierDerivative(curve: PHQuintic, t: Double): Vector2 {
        val p = curve.controlPoints
        val d = List(5) { (p[it + 1] - p[it]) * 5.0 }.toMutableList()
        for (r in 1 until 5) for (i in 0 until 5 - r) d[i] = d[i] * (1.0 - t) + d[i + 1] * t
        return d[0]
    }

    @Test
    fun hermiteInterpolates() {
        val random = Random(1)
        repeat(200) {
            val start = randomPoint(random)
            val end = randomPoint(random)
            val d0 = randomVector(random)
            val d1 = randomVector(random)
            val curve = phQuinticHermite(start, d0, end, d1)
            assertTrue(curve.position(0.0).distanceTo(start) < 1E-9)
            assertTrue(curve.position(1.0).distanceTo(end) < 1E-8, "hermite $it misses its end")
            assertTrue(curve.derivative(0.0).distanceTo(d0) < 1E-8)
            assertTrue(curve.derivative(1.0).distanceTo(d1) < 1E-8)
        }
    }

    @Test
    fun pythagoreanHodograph() {
        // the hodograph of the control points is w², and its length is the polynomial speed |w|²
        val random = Random(2)
        repeat(100) {
            val curve = randomHermite(random)
            for (t in listOf(0.0, 0.2, 0.5, 0.9, 1.0)) {
                val d = bezierDerivative(curve, t)
                assertTrue(d.distanceTo(curve.derivative(t)) < 1E-8 * (1.0 + d.length))
                assertEquals(d.length, curve.speed(t), 1E-8 * (1.0 + d.length))
            }
        }
    }

    @Test
    fun exactArcLength() {
        val random = Random(3)
        repeat(50) {
            val curve = randomHermite(random)
            // 3-point Gauss-Legendre is exact up to degree 5, the speed is a quartic
            val nodes = listOf(-kotlin.math.sqrt(0.6), 0.0, kotlin.math.sqrt(0.6))
            val weights = listOf(5.0 / 9.0, 8.0 / 9.0, 5.0 / 9.0)
            val integral = nodes.indices.sumOf { weights[it] * curve.speed((nodes[it] + 1.0) / 2.0) } / 2.0
            assertEquals(integral, curve.length, 1E-9 * integral)
            for (fraction in listOf(0.1, 0.5, 0.77)) {
                val t = curve.parameterAtLength(fraction * curve.length)
                assertEquals(fraction * curve.length, curve.arcLength(t), 1E-9 * curve.length)
            }
        }
    }

    @Test
    fun straightHermite() {
        val curve = phQuinticHermite(Vector2(0.0, 0.0), Vector2(100.0, 50.0), Vector2(100.0, 50.0), Vector2(100.0, 50.0))
        for (p in curve.controlPoints) assertEquals(0.0, p.x * 50.0 - p.y * 100.0, 1E-9)
        assertEquals(Vector2(100.0, 50.0).length, curve.length, 1E-9)
    }

    @Test
    fun curvatureAndOffsets() {
        val random = Random(4)
        repeat(50) {
            val curve = randomHermite(random)
            for (t in listOf(0.13, 0.5, 0.81)) {
                if (curve.speed(t) < 1.0) continue
                // curvature from finite differences of the tangent angle
                val h = 1E-5
                val a0 = curve.derivative(t - h)
                val a1 = curve.derivative(t + h)
                val turn = kotlin.math.atan2(a0.x * a1.y - a0.y * a1.x, a0.dot(a1))
                val numeric = turn / (curve.arcLength(t + h) - curve.arcLength(t - h))
                assertEquals(numeric, curve.curvature(t), 1E-5 * (1.0 + abs(numeric)))
                for (d in listOf(-12.0, 7.0)) {
                    val delta = curve.offset(t, d) - curve.position(t)
                    assertEquals(abs(d), delta.length, 1E-9)
                    assertEquals(0.0, delta.dot(curve.tangent(t)), 1E-9)
                    val numericDerivative = (curve.offset(t + h, d) - curve.offset(t - h, d)) / (2 * h)
                    val exact = curve.offsetDerivative(t, d)
                    assertTrue(numericDerivative.distanceTo(exact) < 1E-4 * (1.0 + exact.length))
                }
            }
        }
    }

    @Test
    fun cubicApproximation() {
        val random = Random(5)
        repeat(30) {
            val curve = randomHermite(random)
            for (offset in listOf(0.0, 5.0)) {
                val segments = curve.segments(offset, tolerance = 0.01)
                assertTrue(segments.first().start.distanceTo(curve.offset(0.0, offset)) < 1E-9)
                assertTrue(segments.last().end.distanceTo(curve.offset(1.0, offset)) < 1E-9)
                for (segment in segments) for (u in listOf(0.0, 0.5, 1.0)) {
                    val p = segment.position(u)
                    // distance to the exact curve, by dense sampling refined with ternary search
                    fun distance(t: Double) = curve.offset(t, offset).distanceTo(p)
                    val k = (0..400).minBy { distance(it / 400.0) }
                    var lo = maxOf(0.0, (k - 1) / 400.0)
                    var hi = minOf(1.0, (k + 1) / 400.0)
                    repeat(60) {
                        val m1 = lo + (hi - lo) / 3.0
                        val m2 = hi - (hi - lo) / 3.0
                        if (distance(m1) < distance(m2)) hi = m2 else lo = m1
                    }
                    assertTrue(distance((lo + hi) / 2.0) < 0.02, "piece deviates by ${distance((lo + hi) / 2.0)}")
                }
            }
        }
    }

    /** Asserts that consecutive curves meet with equal derivative and curvature; open splines have zero end curvature. */
    private fun assertC2(curves: List<PHQuintic>, closed: Boolean, message: String) {
        val joints = if (closed) curves.size else curves.size - 1
        for (i in 0 until joints) {
            val a = curves[i]
            val b = curves[(i + 1) % curves.size]
            val scale = 1.0 + a.derivative(1.0).length
            assertTrue(a.end.distanceTo(b.start) < 1E-8 * scale, "$message: joint $i doesn't connect")
            assertTrue(a.derivative(1.0).distanceTo(b.derivative(0.0)) < 1E-8 * scale, "$message: derivative jumps at $i")
            assertEquals(a.curvature(1.0), b.curvature(0.0), 1E-8 * (1.0 + abs(a.curvature(1.0))), "$message: curvature at $i")
        }
        if (!closed) {
            assertEquals(0.0, curves.first().curvature(0.0), 1E-9, "$message: start curvature")
            assertEquals(0.0, curves.last().curvature(1.0), 1E-9, "$message: end curvature")
        }
    }

    @Test
    fun smoothSplines() {
        val blob = List(11) { i ->
            val a = i * 2 * PI / 11
            Vector2(cos(a), sin(a)) * (110.0 + 35.0 * sin(3 * a + 0.4))
        }
        val closed = blob.phQuinticSpline(closed = true)
        assertEquals(11, closed.size)
        for ((i, curve) in closed.withIndex()) assertTrue(curve.start.distanceTo(blob[i]) < 1E-9)
        assertC2(closed, true, "blob")

        val wave = List(9) { i -> Vector2(-180.0 + i * 45.0, 70.0 * sin(i * 0.9)) }
        val open = wave.phQuinticSpline(closed = false)
        assertEquals(8, open.size)
        assertTrue(open.last().end.distanceTo(wave.last()) < 1E-8)
        assertC2(open, false, "wave")
    }

    @Test
    fun randomSplines() {
        val random = Random(6)
        var c2 = 0
        val trials = 100
        repeat(trials) {
            val points = List(3 + it % 8) { randomPoint(random) }
            val closed = it % 2 == 1
            val curves = points.phQuinticSpline(closed)
            // always interpolating, whether C2 or the C1 fallback
            for ((i, curve) in curves.withIndex()) {
                assertTrue(curve.start.distanceTo(points[i]) < 1E-8)
                assertTrue(curve.end.distanceTo(points[(i + 1) % points.size]) < 1E-8, "spline $it misses a point")
            }
            val isC2 = runCatching { assertC2(curves, closed, "") }.isSuccess
            if (isC2) c2++
        }
        // all 100 are C2 at the time of writing
        assertTrue(c2 >= 95, "only $c2 of $trials random PH splines are C2")
    }

    @Test
    fun contours() {
        val points = List(6) { i -> Vector2(cos(i * PI / 3), sin(i * PI / 3)) * 100.0 }
        val contour = points.phQuintics(closed = true)
        assertTrue(contour.closed)
        val ends = contour.segments.map { it.start }
        for (p in points) assertTrue(ends.any { it.distanceTo(p) < 1E-9 })
        val offset = points.phQuintics(closed = true, offset = 10.0)
        assertTrue(offset.closed)
    }
}
