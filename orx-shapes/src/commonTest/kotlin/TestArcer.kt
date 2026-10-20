import org.openrndr.extra.shapes.arcer.arcer
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArcer {

    /** Asserts that consecutive segments connect and share their tangent direction, skipping the closing joint when [seam] is false. */
    private fun assertTangentContinuous(contour: ShapeContour, seam: Boolean, message: String = "") {
        val s = contour.segments
        val joints = if (contour.closed && seam) s.size else s.size - 1
        for (i in 0 until joints) {
            val a = s[i]
            val b = s[(i + 1) % s.size]
            assertTrue(a.end.distanceTo(b.start) < 1E-9, "$message: segments $i and ${i + 1} don't connect")
            val da = a.direction(1.0)
            val db = b.direction(0.0)
            assertTrue(da.distanceTo(db) < 1E-7, "$message: tangent jumps at joint $i, $da vs $db")
        }
    }

    private fun assertPassesThrough(contour: ShapeContour, points: List<Vector2>, message: String = "") {
        val ends = contour.segments.map { it.start } + contour.segments.last().end
        for (p in points) {
            assertTrue(ends.any { it.distanceTo(p) < 1E-9 }, "$message: $p is not on the contour")
        }
    }

    private fun randomPoints(random: Random, n: Int) =
        List(n) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }

    @Test
    fun open() {
        val random = Random(1)
        repeat(50) {
            val points = randomPoints(random, 2 + it % 8)
            val contour = points.arcer(closed = false)
            assertTrue(!contour.closed)
            assertEquals(points.first(), contour.segments.first().start)
            assertEquals(points.last(), contour.segments.last().end)
            assertPassesThrough(contour, points, "open $it")
            assertTangentContinuous(contour, seam = false, "open $it")
        }
    }

    @Test
    fun closedOdd() {
        val random = Random(2)
        repeat(50) {
            val points = randomPoints(random, 3 + 2 * (it % 4))
            val contour = points.arcer(closed = true)
            assertTrue(contour.closed)
            assertPassesThrough(contour, points, "closed $it")
            assertTangentContinuous(contour, seam = true, "closed $it")
        }
    }

    @Test
    fun closedEven() {
        val random = Random(3)
        repeat(20) {
            val points = randomPoints(random, 4 + 2 * (it % 3))
            val contour = points.arcer(closed = true)
            assertPassesThrough(contour, points, "closed even $it")
            assertTangentContinuous(contour, seam = false, "closed even $it")
        }
    }

    @Test
    fun startTangent() {
        val points = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(200.0, 50.0))
        val contour = points.arcer(closed = false, startTangent = Vector2(0.0, 1.0))
        assertTrue(contour.segments.first().direction(0.0).distanceTo(Vector2(0.0, 1.0)) < 1E-9)
        // half circle from (0,0) to (100,0) around (50,0)
        val mid = contour.segments.first().position(1.0)
        assertTrue(abs(mid.distanceTo(Vector2(50.0, 0.0)) - 50.0) < 1E-9)
    }

    @Test
    fun collinear() {
        val points = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(200.0, 0.0))
        val contour = points.arcer(closed = false)
        assertEquals(2, contour.segments.size)
        assertTrue(contour.segments.all { it.linear })
    }

    @Test
    fun offset() {
        val random = Random(5)
        repeat(20) {
            val points = randomPoints(random, 3 + 2 * (it % 4))
            val contour = points.arcer(closed = true)
            for (d in listOf(-7.0, 3.0, 40.0)) {
                val parallel = points.arcer(closed = true, offset = d)
                assertEquals(contour.segments.size, parallel.segments.size)
                for ((a, b) in contour.segments.zip(parallel.segments)) {
                    for (t in listOf(0.0, 0.3, 0.5, 1.0)) {
                        val delta = b.position(t) - a.position(t)
                        val normal = a.direction(t).let { v -> Vector2(-v.y, v.x) }
                        assertTrue((delta - normal * d).length < 1E-3 * abs(d), "offset $it, $d: $delta vs ${normal * d}")
                    }
                }
            }
        }
    }

    @Test
    fun maxArcLengthStraightensLoops() {
        // leaving almost straight back, the arc to (100, 0) is a near full circle of radius far above 50
        val points = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0))
        val tangent = Vector2(-1.0, 0.1).normalized
        val loop = points.arcer(closed = false, startTangent = tangent)
        assertTrue(loop.length > 10 * 100.0)
        val contour = points.arcer(closed = false, startTangent = tangent, maxArcLength = 50.0)
        assertEquals(points.last(), contour.segments.last().end)
        assertTangentContinuous(contour, seam = false, "loop")
        assertTrue(contour.length < 3 * 100.0, "split loop is ${contour.length} long")
        for (segment in contour.segments) assertTrue(segment.length < 50.0 + 1E-3)
    }

    @Test
    fun maxArcLength() {
        val random = Random(6)
        var smooth = 0
        repeat(50) {
            val points = randomPoints(random, 3 + it % 6)
            for (closed in listOf(false, true)) {
                val contour = points.arcer(closed = closed, maxArcLength = 100.0)
                assertPassesThrough(contour, points, "max arc length $it, closed $closed")
                assertTangentContinuous(contour, seam = false, "max arc length $it, closed $closed")
                // every arc is at most 100 long, so are its Bézier pieces
                for (segment in contour.segments.filter { s -> !s.linear }) {
                    assertTrue(segment.length < 100.0 + 1E-3, "max arc length $it: ${segment.length}")
                }
                if (closed) {
                    val a = contour.segments.last().direction(1.0)
                    val b = contour.segments.first().direction(0.0)
                    if (a.distanceTo(b) < 1E-7) smooth++
                }
            }
        }
        assertTrue(smooth > 0, "no closed contour closes smoothly")
    }
}
