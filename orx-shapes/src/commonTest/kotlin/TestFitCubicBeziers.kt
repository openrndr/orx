import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.extra.shapes.fit.toContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFitCubicBeziers {

    /** The largest distance from [points] to the [contour]. */
    private fun maxError(contour: ShapeContour, points: List<Vector2>) =
        points.maxOf { contour.nearest(it).position.distanceTo(it) }

    private fun assertG1(segments: List<Segment2D>, closed: Boolean, except: Set<Int> = emptySet()) {
        val joints = if (closed) segments.size else segments.size - 1
        for (i in 0 until joints) {
            val a = segments[i]
            val b = segments[(i + 1) % segments.size]
            assertTrue(a.end.distanceTo(b.start) < 1E-9, "joint $i doesn't connect")
            if ((i + 1) % segments.size !in except) {
                assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-6, "tangent jumps at joint $i")
            }
        }
    }

    @Test
    fun unevenlySampledBezierIsOnePiece() {
        // samples at uniform parameters of a curve whose speed varies: their chord lengths differ from their parameters,
        // which reparameterization corrects; the samples are dense enough everywhere for good tangent estimates
        val bezier = Segment2D(Vector2(0.0, 0.0), Vector2(80.0, 200.0), Vector2(250.0, -120.0), Vector2(300.0, 60.0))
        val points = List(101) { bezier.position(it / 100.0) }
        val fit = fitCubicBeziers(points, errorTolerance = 0.05)
        assertEquals(1, fit.size)
        assertTrue(maxError(fit.toContour(), points) <= 0.05)
    }

    @Test
    fun closedCircle() {
        val points = List(200) { Vector2(cos(it * 2 * PI / 200), sin(it * 2 * PI / 200)) * 100.0 }
        val fit = fitCubicBeziers(points, errorTolerance = 0.05, closed = true)
        val contour = fit.toContour(closed = true)
        assertTrue(fit.size <= 8, "${fit.size} pieces for a circle")
        assertTrue(fit.last().end.distanceTo(fit.first().start) < 1E-9)
        assertG1(fit, closed = true)
        assertTrue(maxError(contour, points) <= 0.05)
    }

    @Test
    fun corners() {
        // a rounded-off square would miss its corners; with a corner angle they stay sharp
        val square = listOf(Vector2(0.0, 0.0), Vector2(200.0, 0.0), Vector2(200.0, 200.0), Vector2(0.0, 200.0))
        val points = square.indices.flatMap { i ->
            val a = square[i]
            val b = square[(i + 1) % 4]
            List(20) { k -> a.mix(b, k / 20.0) }
        }
        for (closed in listOf(true)) {
            val fit = fitCubicBeziers(points, errorTolerance = 0.1, cornerAngle = 45.0, closed = closed)
            val contour = fit.toContour(closed)
            assertTrue(maxError(contour, points) <= 0.1)
            for (corner in square) {
                val joint = fit.indexOfFirst { it.start.distanceTo(corner) < 1E-9 }
                assertTrue(joint >= 0, "$corner is not a joint")
                val before = fit[(joint - 1 + fit.size) % fit.size].direction(1.0)
                val after = fit[joint].direction(0.0)
                assertEquals(90.0, acos(before.dot(after).coerceIn(-1.0, 1.0)) * 180.0 / PI, 1E-6)
            }
        }
        // without a corner angle the corners are rounded and the fit is G1 everywhere
        val smooth = fitCubicBeziers(points, errorTolerance = 0.1, closed = true)
        assertG1(smooth, closed = true)
    }

    @Test
    fun openWithinTolerance() {
        val random = Random(2)
        val points = List(300) { i -> Vector2(i * 2.0, 80.0 * sin(i * 0.05) + 30.0 * sin(i * 0.17) + random.nextDouble(-0.5, 0.5)) }
        for (tolerance in listOf(0.5, 2.0, 5.0)) {
            val fit = fitCubicBeziers(points, errorTolerance = tolerance)
            assertG1(fit, closed = false)
            assertTrue(fit.first().start.distanceTo(points.first()) < 1E-9)
            assertTrue(fit.last().end.distanceTo(points.last()) < 1E-9)
            assertTrue(maxError(fit.toContour(), points) <= tolerance)
        }
        // corners in an open polyline: a V with its apex at point 30, which turns by 90 degrees
        val v = List(31) { Vector2(it * 5.0, it * 5.0) } + List(30) { Vector2(155.0 + it * 5.0, 145.0 - it * 5.0) }
        val fitV = fitCubicBeziers(v, errorTolerance = 0.1, cornerAngle = 60.0)
        assertTrue(maxError(fitV.toContour(), v) <= 0.1)
        val apex = fitV.indexOfFirst { it.start.distanceTo(v[30]) < 1E-9 }
        assertTrue(apex > 0, "the apex is not a joint")
        assertTrue(fitV[apex - 1].direction(1.0).dot(fitV[apex].direction(0.0)) < 0.01, "the apex is rounded")
        assertEquals(1, fitCubicBeziers(listOf(Vector2.ZERO, Vector2(10.0, 0.0))).size)
    }
}
