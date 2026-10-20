import org.openrndr.extra.shapes.splines.TCB
import org.openrndr.extra.shapes.splines.kochanekBartels
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestKochanekBartels {

    private val points = listOf(
        Vector2(0.0, 0.0), Vector2(100.0, 40.0), Vector2(180.0, -20.0), Vector2(260.0, 60.0), Vector2(320.0, 0.0)
    )

    /** The tangents with which the cubic spline [segments] arrives at and leaves joint i, as Hermite derivatives. */
    private fun tangents(contour: org.openrndr.shape.ShapeContour, i: Int): Pair<Vector2, Vector2> {
        val s = contour.segments
        val arriving = (s[i - 1].end - s[i - 1].control[1]) * 3.0
        val leaving = (s[i].control[0] - s[i].start) * 3.0
        return Pair(arriving, leaving)
    }

    @Test
    fun interpolates() {
        val random = Random(1)
        repeat(20) {
            val ps = List(3 + it % 6) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
            for (closed in listOf(false, true)) {
                val contour = ps.kochanekBartels(closed, random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0))
                assertEquals(closed, contour.closed)
                assertEquals(if (closed) ps.size else ps.size - 1, contour.segments.size)
                for ((i, segment) in contour.segments.withIndex()) {
                    assertTrue(segment.start.distanceTo(ps[i]) < 1E-12)
                    assertTrue(segment.end.distanceTo(ps[(i + 1) % ps.size]) < 1E-12)
                }
            }
        }
    }

    @Test
    fun defaultIsCatmullRom() {
        val contour = points.kochanekBartels(closed = false)
        for (i in 1 until points.size - 1) {
            val (arriving, leaving) = tangents(contour, i)
            val expected = (points[i + 1] - points[i - 1]) / 2.0
            assertTrue(arriving.distanceTo(expected) < 1E-9)
            assertTrue(leaving.distanceTo(expected) < 1E-9)
        }
        // the open ends leave and arrive along the end edges
        assertTrue((contour.segments.first().control[0] - points[0]).normalized.distanceTo((points[1] - points[0]).normalized) < 1E-12)
    }

    @Test
    fun tension() {
        // tension 1 gives zero tangents: every edge is a straight segment
        val contour = points.kochanekBartels(closed = false, tension = 1.0)
        for (segment in contour.segments) {
            assertTrue(segment.control.all { it.distanceTo(segment.start) < 1E-12 || it.distanceTo(segment.end) < 1E-12 })
        }
        // tension 0.5 halves the Catmull-Rom tangents
        val half = points.kochanekBartels(closed = false, tension = 0.5)
        val (_, leaving) = tangents(half, 2)
        assertTrue(leaving.distanceTo((points[3] - points[1]) / 4.0) < 1E-9)
    }

    @Test
    fun continuity() {
        // continuity -1 gives a corner: the curve arrives along the incoming edge and leaves along the outgoing one
        val corner = points.kochanekBartels(closed = false, continuity = -1.0)
        for (i in 1 until points.size - 1) {
            val (arriving, leaving) = tangents(corner, i)
            assertTrue(arriving.distanceTo(points[i] - points[i - 1]) < 1E-9)
            assertTrue(leaving.distanceTo(points[i + 1] - points[i]) < 1E-9)
        }
        // any other continuity than 0 breaks C1, 0 keeps it
        val smooth = points.kochanekBartels(closed = true, tension = 0.3, bias = -0.4)
        for (i in 1 until points.size) {
            val (arriving, leaving) = tangents(smooth, i)
            assertTrue(arriving.distanceTo(leaving) < 1E-9)
        }
    }

    @Test
    fun bias() {
        // bias 1 takes the tangent along the incoming edge, -1 along the outgoing one
        val before = points.kochanekBartels(closed = false, bias = 1.0)
        val after = points.kochanekBartels(closed = false, bias = -1.0)
        for (i in 1 until points.size - 1) {
            assertTrue(tangents(before, i).second.distanceTo(points[i] - points[i - 1]) < 1E-9)
            assertTrue(tangents(after, i).second.distanceTo(points[i + 1] - points[i]) < 1E-9)
        }
    }

    @Test
    fun perPointParameters() {
        val parameters = List(points.size) { if (it == 2) TCB(continuity = -1.0) else TCB() }
        val contour = points.kochanekBartels(closed = false, parameters)
        val (arriving, leaving) = tangents(contour, 2)
        assertTrue(arriving.distanceTo(points[2] - points[1]) < 1E-9)
        assertTrue(leaving.distanceTo(points[3] - points[2]) < 1E-9)
        val (a1, l1) = tangents(contour, 1)
        assertTrue(a1.distanceTo(l1) < 1E-9)
    }

    @Test
    fun threeDimensions() {
        val path = listOf(Vector3(0.0, 0.0, 0.0), Vector3(1.0, 2.0, 3.0), Vector3(4.0, 1.0, 0.0), Vector3(2.0, -1.0, 5.0))
        val spline = path.kochanekBartels(closed = true, tension = 0.2)
        assertTrue(spline.closed)
        assertEquals(4, spline.segments.size)
        for ((i, segment) in spline.segments.withIndex()) {
            assertTrue(segment.start.distanceTo(path[i]) < 1E-12)
            assertTrue(segment.end.distanceTo(path[(i + 1) % 4]) < 1E-12)
        }
        val arriving = (spline.segments[1].end - spline.segments[1].control[1]) * 3.0
        val leaving = (spline.segments[2].control[0] - spline.segments[2].start) * 3.0
        assertTrue(arriving.distanceTo((path[3] - path[1]) * 0.4) < 1E-9)
        assertTrue(leaving.distanceTo(arriving) < 1E-9)
    }
}
