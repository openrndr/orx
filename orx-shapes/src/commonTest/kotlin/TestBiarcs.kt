import org.openrndr.extra.shapes.biarc.BiarcTangents
import org.openrndr.extra.shapes.biarc.biarc
import org.openrndr.extra.shapes.biarc.biarcs
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBiarcs {

    /** Asserts that consecutive segments connect and share their tangent direction, including the closing joint. */
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

    private fun randomDirection(random: Random) = random.nextDouble(0.0, 2 * PI).let { Vector2(cos(it), sin(it)) }

    @Test
    fun singleBiarcMatchesEndTangents() {
        val random = Random(1)
        repeat(200) {
            val start = randomPoints(random, 1)[0]
            val end = randomPoints(random, 1)[0]
            val t0 = randomDirection(random)
            val t1 = randomDirection(random)
            val contour = biarc(start, t0, end, t1)
            assertTangentContinuous(contour, "biarc $it")
            assertTrue(contour.segments.first().start.distanceTo(start) < 1E-9)
            assertTrue(contour.segments.last().end.distanceTo(end) < 1E-9)
            assertTrue(contour.segments.first().direction(0.0).distanceTo(t0) < 1E-7, "biarc $it start tangent")
            assertTrue(contour.segments.last().direction(1.0).distanceTo(t1) < 1E-7, "biarc $it end tangent")
        }
    }

    @Test
    fun equalTangents() {
        // parallel tangents across the chord give two semicircles
        val contour = biarc(Vector2(0.0, 0.0), Vector2(0.0, 1.0), Vector2(100.0, 0.0), Vector2(0.0, 1.0))
        assertTangentContinuous(contour, "semicircles")
        assertEquals(PI * 50.0, contour.length, PI * 50.0 * 3E-4)
        assertTrue(contour.segments.last().direction(1.0).distanceTo(Vector2(0.0, 1.0)) < 1E-9)
        // tangents along the chord give a straight segment
        val line = biarc(Vector2(0.0, 0.0), Vector2(1.0, 0.0), Vector2(100.0, 0.0), Vector2(1.0, 0.0))
        assertTrue(line.segments.all { it.linear })
        assertEquals(100.0, line.length, 1E-9)
    }

    @Test
    fun polylines() {
        val random = Random(2)
        repeat(50) {
            val points = randomPoints(random, 2 + it % 8)
            for (closed in listOf(false, true)) {
                if (closed && points.size < 3) continue
                val contour = points.biarcs(closed, tangentMethod = BiarcTangents.Circle)
                assertEquals(closed, contour.closed)
                assertTangentContinuous(contour, "polyline $it, closed $closed")
                val ends = contour.segments.map { s -> s.start } + contour.segments.last().end
                for (p in points) assertTrue(ends.any { e -> e.distanceTo(p) < 1E-9 }, "polyline $it: $p missed")
            }
        }
    }

    @Test
    fun givenTangents() {
        val random = Random(3)
        val points = randomPoints(random, 6)
        val tangents = List(points.size) { randomDirection(random) }
        val contour = points.biarcs(closed = true, tangents = tangents)
        assertTangentContinuous(contour, "given tangents")
        for ((p, t) in points.zip(tangents)) {
            val segment = contour.segments.first { it.start.distanceTo(p) < 1E-9 }
            assertTrue(segment.direction(0.0).distanceTo(t) < 1E-7)
        }
    }

    @Test
    fun pointsOnACircle() {
        val random = Random(4)
        val center = Vector2(200.0, 200.0)
        val angles = List(9) { random.nextDouble(0.0, 2 * PI) }.sorted()
        val points = angles.map { center + Vector2(cos(it), sin(it)) * 100.0 }
        val contour = points.biarcs(closed = true, tangentMethod = BiarcTangents.Circle)
        for (segment in contour.segments) for (t in listOf(0.0, 0.25, 0.5, 0.75, 1.0)) {
            assertEquals(100.0, segment.position(t).distanceTo(center), 100.0 * 3E-4)
        }
    }

    @Test
    fun collinear() {
        val points = List(5) { Vector2(it * 50.0, it * 20.0) }
        val contour = points.biarcs(closed = false)
        assertTrue(contour.segments.all { it.linear })
    }

    @Test
    fun offset() {
        val random = Random(5)
        val points = randomPoints(random, 7)
        val contour = points.biarcs(closed = true)
        for (d in listOf(-6.0, 4.0)) {
            val parallel = points.biarcs(closed = true, offset = d)
            assertEquals(contour.segments.size, parallel.segments.size)
            for ((a, b) in contour.segments.zip(parallel.segments)) for (t in listOf(0.0, 0.5, 1.0)) {
                val normal = a.direction(t).let { v -> Vector2(-v.y, v.x) }
                assertTrue((b.position(t) - a.position(t) - normal * d).length < 1E-3 * abs(d))
            }
        }
    }

    @Test
    fun catmullRomTangents() {
        val random = Random(6)
        repeat(50) {
            val points = randomPoints(random, 2 + it % 8)
            for (alpha in listOf(0.0, 0.5, 1.0)) for (closed in listOf(false, true)) {
                if (closed && points.size < 3) continue
                val contour = points.biarcs(closed, tangentMethod = BiarcTangents.CatmullRom(alpha))
                assertTangentContinuous(contour, "catmull-rom $it, alpha $alpha, closed $closed")
                val ends = contour.segments.map { s -> s.start } + contour.segments.last().end
                for (p in points) assertTrue(ends.any { e -> e.distanceTo(p) < 1E-9 })
            }
        }
    }

    @Test
    fun uniformCatmullRomTangents() {
        // with uniform knots the tangent at a point is along the chord between its neighbours
        val points = listOf(Vector2(0.0, 0.0), Vector2(100.0, 20.0), Vector2(150.0, 120.0), Vector2(60.0, 200.0))
        val contour = points.biarcs(closed = true, tangentMethod = BiarcTangents.CatmullRom(0.0))
        for (i in points.indices) {
            val expected = (points[(i + 1) % 4] - points[(i + 3) % 4]).normalized
            val segment = contour.segments.first { it.start.distanceTo(points[i]) < 1E-9 }
            assertTrue(segment.direction(0.0).distanceTo(expected) < 1E-7, "point $i")
        }
        // the end edges of an open polyline are single arcs: the start tangent is the next one mirrored in the chord
        val open = points.biarcs(closed = false, tangentMethod = BiarcTangents.CatmullRom(0.0))
        val t1 = (points[2] - points[0]).normalized
        val c = (points[1] - points[0]).normalized
        val t0 = c * (2.0 * t1.dot(c)) - t1
        assertTrue(open.segments.first().direction(0.0).distanceTo(t0) < 1E-7)
    }
}
