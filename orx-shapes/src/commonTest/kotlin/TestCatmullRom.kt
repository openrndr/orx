import org.openrndr.extra.shapes.splines.CatmullRomChain2
import org.openrndr.extra.shapes.splines.CatmullRomChain3
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toContour
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.extra.shapes.splines.toSegment
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCatmullRom {

    private val points = listOf(
        Vector2(0.0, 0.0), Vector2(100.0, 80.0), Vector2(200.0, -20.0), Vector2(300.0, 90.0), Vector2(380.0, 10.0)
    )

    @Test
    fun closedThreeDimensionsGoesRoundOnce() {
        val chain = points.map { Vector3(it.x, it.y, 0.0) }.catmullRom(closed = true)
        assertEquals(points.size, chain.segments.size)
        assertEquals(points.size, chain.toPath3D().segments.size)
        // planar 3D points give the 2D curve
        val contour = points.catmullRom(closed = true).toContour()
        for ((s2, s3) in contour.segments.zip(chain.toPath3D().segments)) {
            for ((p2, p3) in (s2.control + s2.start + s2.end).zip(s3.control + s3.start + s3.end)) {
                assertTrue(Vector3(p2.x, p2.y, 0.0).distanceTo(p3) < 1E-9)
            }
        }
    }

    @Test
    fun scaleInvariant() {
        for (alpha in listOf(0.0, 0.5, 1.0)) for (closed in listOf(false, true)) {
            val large = points.catmullRom(alpha, closed).toContour()
            val small = points.map { it / 100.0 }.catmullRom(alpha, closed).toContour()
            for (k in 0..100) {
                val t = k / 100.0
                assertTrue((small.position(t) * 100.0).distanceTo(large.position(t)) < 1E-9, "alpha $alpha, closed $closed")
            }
        }
    }

    @Test
    fun repeatedPoints() {
        val repeated = listOf(points[0]) + points.flatMap { listOf(it, it) }
        for (closed in listOf(false, true)) {
            val contour = repeated.catmullRom(closed = closed).toContour()
            val expected = points.catmullRom(closed = closed).toContour()
            assertEquals(expected.segments.size, contour.segments.size)
            for ((a, b) in contour.segments.zip(expected.segments)) {
                for ((p, q) in (a.control + a.start + a.end).zip(b.control + b.start + b.end)) {
                    assertTrue(!p.x.isNaN() && p.distanceTo(q) < 1E-9)
                }
            }
        }
    }

    @Test
    fun interpolatesAndIsC1() {
        val random = Random(1)
        repeat(20) {
            val ps = List(3 + it % 6) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
            for (closed in listOf(false, true)) {
                val contour = ps.catmullRom(closed = closed).toContour()
                assertEquals(if (closed) ps.size else ps.size - 1, contour.segments.size)
                val joints = contour.segments.map { s -> s.start } + contour.segments.last().end
                for (p in ps) assertTrue(joints.any { j -> j.distanceTo(p) < 1E-9 })
                val count = if (closed) contour.segments.size else contour.segments.size - 1
                for (i in 0 until count) {
                    val a = contour.segments[i]
                    val b = contour.segments[(i + 1) % contour.segments.size]
                    assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-9, "tangent jumps at $i")
                }
            }
        }
    }

    @Test
    fun beziersMatchTheSpline() {
        val chain = CatmullRomChain2(points, 0.5, false)
        for (segment in chain.segments) {
            val bezier = segment.toSegment()
            val exact = (0..400).map { segment.position(it / 400.0) }
            for (k in 0..40) {
                val q = bezier.position(k / 40.0)
                assertTrue(exact.minOf { it.distanceTo(q) } < 0.5)
            }
        }
        // the open ends mirror the end edges: with uniform knots the curve leaves along the first edge
        val uniform = CatmullRomChain2(points, 0.0, false).toContour()
        val start = uniform.segments.first()
        assertTrue((start.control[0] - start.start).normalized.distanceTo((points[1] - points[0]).normalized) < 1E-9)
        assertTrue(CatmullRomChain3(listOf(Vector3.ZERO), 0.5, false).segments.isEmpty())
    }
}
