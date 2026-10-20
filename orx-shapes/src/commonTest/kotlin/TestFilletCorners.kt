import org.openrndr.extra.shapes.metro.metro
import org.openrndr.extra.shapes.operators.filletCorners
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFilletCorners {

    /** Rounds the corners of a polyline contour, such as the output of [metro]. */
    private fun ShapeContour.filleted(radius: Double) =
        (segments.map { it.start } + if (closed) emptyList() else listOf(segments.last().end))
            .filletCorners(radius, closed, polarity = polarity)

    /** Asserts that consecutive segments connect and share their tangent direction. */
    private fun assertTangentContinuous(contour: ShapeContour, message: String = "") {
        val s = contour.segments
        val joints = if (contour.closed) s.size else s.size - 1
        for (i in 0 until joints) {
            val a = s[i]
            val b = s[(i + 1) % s.size]
            assertTrue(a.end.distanceTo(b.start) < 1E-9, "$message: segments $i and ${i + 1} don't connect")
            val da = a.direction(1.0)
            val db = b.direction(0.0)
            assertTrue(da.distanceTo(db) < 1E-9, "$message: tangent jumps at joint $i, $da vs $db")
        }
    }

    @Test
    fun square() {
        val square = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0), Vector2(0.0, 100.0))
        val contour = square.filletCorners(10.0, closed = true)
        assertTrue(contour.closed)
        assertEquals(8, contour.segments.size)
        assertTangentContinuous(contour, "square")
        assertEquals(4 * 80.0 + 2 * PI * 10.0, contour.length, 0.05)

        // the arc at (100, 0) is centered on (90, 10)
        val arc = contour.segments.first { !it.linear && it.start.x > 50.0 && it.start.y < 50.0 }
        for (t in 0..10) {
            assertEquals(10.0, arc.position(t / 10.0).distanceTo(Vector2(90.0, 10.0)), 10.0 * 3E-4)
        }
    }

    @Test
    fun sharpCornersAreAccurate() {
        // a 160 degree turn needs two Bézier pieces
        val points = listOf(Vector2(0.0, 0.0), Vector2(200.0, 0.0), Vector2(200.0 - 200.0 * cos(PI / 9), 200.0 * sin(PI / 9)))
        val contour = points.filletCorners(20.0, closed = false)
        assertTangentContinuous(contour, "sharp")
        assertEquals(points.first(), contour.segments.first().start)
        assertEquals(points.last(), contour.segments.last().end)
        val arcs = contour.segments.filter { !it.linear }
        assertEquals(2, arcs.size)
        val center = Vector2(200.0 - 20.0 / kotlin.math.tan(PI / 18), 20.0)
        for (arc in arcs) for (t in 0..10) {
            assertEquals(20.0, arc.position(t / 10.0).distanceTo(center), 20.0 * 3E-4)
        }
    }

    @Test
    fun largeRadiusIsClamped() {
        val zigzag = List(10) { Vector2(it * 10.0, if (it % 2 == 0) 0.0 else 10.0) }
        val contour = zigzag.filletCorners(1000.0, closed = false)
        assertTangentContinuous(contour, "zigzag")
        assertEquals(zigzag.first(), contour.segments.first().start)
        assertEquals(zigzag.last(), contour.segments.last().end)
        // no line runs backwards
        for (s in contour.segments.filter { it.linear }) assertTrue(s.end.x >= s.start.x - 1E-9, "backwards line $s")
    }

    @Test
    fun degenerateCorners() {
        // collinear and reversing corners stay sharp, duplicate points are ignored
        val points = listOf(Vector2(0.0, 0.0), Vector2(50.0, 0.0), Vector2(50.0, 0.0), Vector2(100.0, 0.0), Vector2(30.0, 0.0))
        val contour = points.filletCorners(10.0, closed = false)
        assertTrue(contour.segments.all { it.linear })
        assertEquals(Vector2(30.0, 0.0), contour.segments.last().end)
    }

    @Test
    fun metroCornerRadius() {
        val random = Random(2)
        for (trial in 0 until 20) {
            var position = Vector2.ZERO
            var heading = 0.0
            val points = List(200) {
                heading = heading * 0.95 + random.nextDouble(-0.6, 0.6)
                position += Vector2(cos(heading), sin(heading)) * random.nextDouble(2.0, 8.0)
                position
            }
            val path = ShapeContour.fromPoints(points, closed = false)
            for (directions in 2..6) for (radius in listOf(2.0, 10.0, 1000.0)) {
                val contour = path.metro(directions = directions).filleted(radius)
                assertTangentContinuous(contour, "trial $trial, $directions directions, radius $radius")
            }
        }
        val circle = Circle(0.0, 0.0, 100.0).contour.metro(directions = 3).filleted(20.0)
        assertTrue(circle.closed)
        assertTangentContinuous(circle, "circle")
    }

    /** The radius of the circle through the start, middle and end of [segment]. */
    private fun radius(segment: org.openrndr.shape.Segment2D): Double {
        val a = segment.start
        val b = segment.position(0.5)
        val c = segment.end
        val area2 = abs((b - a).x * (c - a).y - (b - a).y * (c - a).x)
        return a.distanceTo(b) * b.distanceTo(c) * c.distanceTo(a) / (2.0 * area2)
    }

    @Test
    fun jogBecomesSBend() {
        // a 10 unit jog between two horizontal lines can't hold two fillets of radius 20
        val jog = listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 10.0), Vector2(200.0, 10.0))
        val contour = jog.filletCorners(20.0, closed = false)
        assertTangentContinuous(contour, "jog")
        val arcs = contour.segments.filter { !it.linear }
        assertEquals(2, arcs.size)
        for (arc in arcs) assertEquals(20.0, radius(arc), 20.0 * 3E-4)

        // symmetric S: offset 10 = 2r(1 - cos α), the inflection at the middle of the jog
        val half = 20.0 * sin(acos(1.0 - 10.0 / 40.0))
        assertTrue(arcs[0].start.distanceTo(Vector2(100.0 - half, 0.0)) < 1E-9, "entry ${arcs[0].start}")
        assertTrue(arcs[0].end.distanceTo(Vector2(100.0, 5.0)) < 1E-9, "inflection ${arcs[0].end}")
        assertTrue(arcs[1].end.distanceTo(Vector2(100.0 + half, 10.0)) < 1E-9, "exit ${arcs[1].end}")
        // the arcs turn in opposite directions
        val turn0 = arcs[0].direction(0.0).let { d -> arcs[0].direction(1.0).let { d.x * it.y - d.y * it.x } }
        val turn1 = arcs[1].direction(0.0).let { d -> arcs[1].direction(1.0).let { d.x * it.y - d.y * it.x } }
        assertTrue(turn0 * turn1 < 0.0, "turns $turn0 and $turn1")

        // without reverse curves the fillets shrink to radius 5
        val fillets = jog.filletCorners(20.0, closed = false, reverseCurves = false)
        assertTangentContinuous(fillets, "fillets")
        for (arc in fillets.segments.filter { !it.linear }) assertEquals(5.0, radius(arc), 5.0 * 3E-4)
    }

    @Test
    fun sBendIsContinuousWithFillets() {
        // an octilinear step whose diagonal is just shorter or just longer than two fillets need
        val t = 20.0 * tan(PI / 8)
        fun step(diagonal: Double): ShapeContour {
            val d = diagonal / sqrt(2.0)
            return listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0 + d, d), Vector2(200.0 + d, d))
                .filletCorners(20.0, closed = false)
        }
        val shorter = step(2 * t - 1E-6)
        val longer = step(2 * t + 1E-6)
        assertTangentContinuous(shorter, "S-bend")
        assertTangentContinuous(longer, "fillets")
        for (i in 0..100) {
            val u = i / 100.0
            assertTrue(shorter.position(u).distanceTo(longer.position(u)) < 1E-4, "at $u")
        }
        for (arc in shorter.segments.filter { !it.linear }) assertEquals(20.0, radius(arc), 20.0 * 3E-4)
    }

    @Test
    fun closedStaircase() {
        // a closed staircase where every step is a jog, including across the closing corner
        val points = mutableListOf<Vector2>()
        for (k in 0 until 6) {
            points.add(Vector2(k * 40.0, k * 8.0))
            points.add(Vector2(k * 40.0 + 30.0, k * 8.0))
        }
        points.add(Vector2(240.0, 48.0))
        points.add(Vector2(240.0, 200.0))
        points.add(Vector2(-10.0, 200.0))
        points.add(Vector2(-10.0, 0.0))
        val contour = points.filletCorners(15.0, closed = true)
        assertTrue(contour.closed)
        assertTangentContinuous(contour, "staircase")
        val shifted = (points.drop(1) + points.first()).filletCorners(15.0, closed = true)
        assertEquals(contour.length, shifted.length, 1E-6)
    }
}
