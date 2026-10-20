import org.openrndr.extra.shapes.clothoid.clothoid
import org.openrndr.extra.shapes.clothoid.clothoidFits
import org.openrndr.extra.shapes.clothoid.clothoids
import org.openrndr.extra.shapes.clothoid.fitClothoid
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestClothoids {

    private fun assertTangentContinuous(contour: ShapeContour, message: String = "") {
        val s = contour.segments
        val joints = if (contour.closed) s.size else s.size - 1
        for (i in 0 until joints) {
            val a = s[i]
            val b = s[(i + 1) % s.size]
            assertTrue(a.end.distanceTo(b.start) < 1E-9, "$message: segments $i and ${i + 1} don't connect")
            assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-7, "$message: tangent jumps at joint $i")
        }
    }

    private fun randomPoints(random: Random, n: Int) =
        List(n) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }

    private fun direction(angle: Double) = Vector2(cos(angle), sin(angle))

    /** The difference between two angles, wrapped to (-π, π]. */
    private fun angleDifference(a: Double, b: Double) = atan2(sin(a - b), cos(a - b))

    @Test
    fun fitMatchesEnds() {
        val random = Random(1)
        repeat(500) {
            val (start, end) = randomPoints(random, 2)
            val theta0 = random.nextDouble(-PI, PI)
            val theta1 = random.nextDouble(-PI, PI)
            val fit = fitClothoid(start, theta0, end, theta1)
            assertTrue(fit.length >= start.distanceTo(end) - 1E-9, "fit $it is shorter than its chord")
            assertTrue(fit.position(fit.length).distanceTo(end) < 1E-8, "fit $it misses its end")
            assertEquals(0.0, angleDifference(fit.angle(0.0), theta0), 1E-12)
            assertEquals(0.0, angleDifference(fit.angle(fit.length), theta1), 1E-8, "fit $it end angle")
        }
    }

    @Test
    fun specialCases() {
        // tangents along the chord give a straight line
        val line = fitClothoid(Vector2(0.0, 0.0), 0.0, Vector2(100.0, 0.0), 0.0)
        assertEquals(100.0, line.length, 1E-9)
        assertEquals(0.0, line.kappa, 1E-12)
        assertEquals(0.0, line.dkappa, 1E-12)
        // mirrored tangents give a circular arc: a half circle of radius 50
        val arc = fitClothoid(Vector2(0.0, 0.0), PI / 2, Vector2(100.0, 0.0), -PI / 2)
        assertEquals(50.0 * PI, arc.length, 1E-8)
        assertEquals(-1.0 / 50.0, arc.kappa, 1E-10)
        assertEquals(0.0, arc.dkappa, 1E-12)
    }

    @Test
    fun bezierApproximation() {
        val random = Random(2)
        var worst = 0.0
        repeat(100) {
            val (start, end) = randomPoints(random, 2)
            val t0 = direction(random.nextDouble(-PI, PI))
            val t1 = direction(random.nextDouble(-PI, PI))
            val contour = clothoid(start, t0, end, t1)
            val fit = fitClothoid(start, atan2(t0.y, t0.x), end, atan2(t1.y, t1.x))
            assertTangentContinuous(contour, "clothoid $it")
            assertTrue(contour.segments.last().end.distanceTo(end) < 1E-9)
            assertTrue(contour.segments.first().direction(0.0).distanceTo(t0) < 1E-9)
            assertTrue(contour.segments.last().direction(1.0).distanceTo(t1) < 1E-7)
            // the Bézier pieces cover equal arc lengths; every piece stays close to the clothoid
            val pieces = contour.segments.size
            for ((j, segment) in contour.segments.withIndex()) {
                val h = fit.length / pieces
                val q = segment.position(0.5)
                fun distance(s: Double) = fit.position(s).distanceTo(q)
                val k = (0..100).minBy { k -> distance(h * (j + k / 100.0)) }
                var lo = h * (j + (k - 1) / 100.0)
                var hi = h * (j + (k + 1) / 100.0)
                repeat(100) {
                    val m1 = lo + (hi - lo) / 3.0
                    val m2 = hi - (hi - lo) / 3.0
                    if (distance(m1) < distance(m2)) hi = m2 else lo = m1
                }
                worst = maxOf(worst, distance((lo + hi) / 2.0) / h)
            }
        }
        assertTrue(worst < 1E-5)
    }

    /** Asserts that the curvature is continuous at every point, and zero at the ends of an open polyline. */
    private fun assertCurvatureContinuous(points: List<Vector2>, closed: Boolean, message: String) {
        val fits = points.clothoidFits(closed)
        val scale = fits.sumOf { it.length } / fits.size
        for (i in fits.indices) {
            if (!closed && i == 0) continue
            val jump = fits[(i - 1 + fits.size) % fits.size].endKappa - fits[i].kappa
            assertTrue(abs(jump) * scale < 1E-8, "$message: curvature jumps by $jump at point $i")
        }
        if (!closed) {
            assertEquals(0.0, fits.first().kappa * scale, 1E-8, "$message: start curvature")
            assertEquals(0.0, fits.last().endKappa * scale, 1E-8, "$message: end curvature")
        }
    }

    @Test
    fun splines() {
        val random = Random(3)
        repeat(50) {
            val points = randomPoints(random, 3 + it % 8)
            for (closed in listOf(false, true)) {
                val contour = points.clothoids(closed)
                assertEquals(closed, contour.closed)
                assertTangentContinuous(contour, "spline $it, closed $closed")
                val ends = contour.segments.map { s -> s.start } + contour.segments.last().end
                for (p in points) assertTrue(ends.any { e -> e.distanceTo(p) < 1E-9 }, "spline $it misses $p")
            }
        }
    }

    @Test
    fun smoothSplinesAreG2() {
        // points along a wavy closed curve and an open wave, spaced closely enough for the spline to follow them
        val closed = List(12) { i ->
            val a = i * 2 * PI / 12
            Vector2(cos(a), sin(a)) * (100.0 + 20.0 * sin(3 * a))
        }
        assertCurvatureContinuous(closed, true, "closed wave")
        val open = List(10) { i -> Vector2(i * 40.0, 60.0 * sin(i * 0.8)) }
        assertCurvatureContinuous(open, false, "open wave")
    }

    @Test
    fun randomSplinesAreG2() {
        val random = Random(4)
        var converged = 0
        val trials = 100
        repeat(trials) {
            val points = randomPoints(random, 3 + it % 8)
            val closed = it % 2 == 1
            val fits = points.clothoidFits(closed)
            val scale = fits.sumOf { f -> f.length } / fits.size
            val jumps = fits.indices.filter { i -> closed || i > 0 }.map { i ->
                abs(fits[(i - 1 + fits.size) % fits.size].endKappa - fits[i].kappa) * scale
            }
            if (jumps.all { j -> j < 1E-8 }) converged++
        }
        // 98 of these converge; the others are closed figure-eights of four points
        assertTrue(converged >= 95, "only $converged of $trials random clothoid splines are G2")
    }

    @Test
    fun pointsOnACircle() {
        val center = Vector2(200.0, 200.0)
        val random = Random(5)
        val angles = List(9) { random.nextDouble(0.0, 2 * PI) }.sorted()
        val points = angles.map { center + direction(it) * 100.0 }
        val contour = points.clothoids(closed = true)
        for (segment in contour.segments) for (t in listOf(0.0, 0.25, 0.5, 0.75, 1.0)) {
            assertEquals(100.0, segment.position(t).distanceTo(center), 100.0 * 1E-5)
        }
    }

    @Test
    fun collinear() {
        val points = List(5) { Vector2(it * 50.0, it * 20.0) }
        val fits = points.clothoidFits(closed = false)
        for (fit in fits) {
            assertEquals(0.0, fit.kappa, 1E-12)
            assertEquals(0.0, fit.dkappa, 1E-12)
        }
    }

    @Test
    fun givenTangents() {
        val random = Random(6)
        val points = randomPoints(random, 6)
        val tangents = List(points.size) { direction(random.nextDouble(-PI, PI)) }
        val contour = points.clothoids(closed = true, tangents = tangents)
        assertTangentContinuous(contour, "given tangents")
        for ((p, t) in points.zip(tangents)) {
            val segment = contour.segments.first { it.start.distanceTo(p) < 1E-9 }
            assertTrue(segment.direction(0.0).distanceTo(t) < 1E-9)
        }
    }
}
