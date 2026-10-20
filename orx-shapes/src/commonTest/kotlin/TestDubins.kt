import org.openrndr.extra.shapes.dubins.dubinsPath
import org.openrndr.extra.shapes.dubins.dubinsPaths
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDubins {

    private fun direction(angle: Double) = Vector2(cos(angle), sin(angle))

    @Test
    fun everyCandidateReachesTheEndPose() {
        val random = Random(1)
        var candidates = 0
        repeat(300) {
            val start = Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0))
            val end = Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0))
            val d0 = direction(random.nextDouble(0.0, 2 * PI))
            val d1 = direction(random.nextDouble(0.0, 2 * PI))
            val radius = random.nextDouble(5.0, 120.0)
            val paths = dubinsPaths(start, d0, end, d1, radius)
            // some words have no solution where the turning circles overlap, but never all
            assertTrue(paths.isNotEmpty())
            for (path in paths) {
                assertTrue(path.end.distanceTo(end) < 1E-7 * (1.0 + radius), "${path.word} misses the end point")
                assertTrue(path.endDirection.distanceTo(d1) < 1E-9, "${path.word} misses the end direction")
                assertTrue(path.lengths.all { l -> l >= 0.0 })
                assertTrue(path.length >= start.distanceTo(end) - 1E-9)
                candidates++
            }
            // shortest first
            assertTrue(paths.zipWithNext().all { (a, b) -> a.length <= b.length })
        }
        assertTrue(candidates > 1200)
    }

    @Test
    fun knownPaths() {
        val r = 10.0
        // straight ahead
        val straight = dubinsPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(40.0, 0.0), Vector2(1.0, 0.0), r)
        assertEquals(40.0, straight.length, 1E-9)
        // a U-turn to the left: a half circle
        val uTurn = dubinsPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(0.0, 2 * r), Vector2(-1.0, 0.0), r)
        assertEquals(PI * r, uTurn.length, 1E-9)
        assertEquals(1, uTurn.word.turns.first())
        // reversing the direction means turning by π at a curvature of at most 1/r, which takes a length of πr at least
        val around = dubinsPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(0.1, 0.0), Vector2(-1.0, 0.0), r)
        assertTrue(around.length >= PI * r - 1E-9)
        assertTrue(around.end.distanceTo(Vector2(0.1, 0.0)) < 1E-9)
        // equal poses
        assertEquals(0.0, dubinsPath(Vector2(3.0, 4.0), Vector2(0.0, 1.0), Vector2(3.0, 4.0), Vector2(0.0, 1.0), r).length)
    }

    @Test
    fun scaleInvariance() {
        val random = Random(2)
        repeat(100) {
            val start = Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0))
            val end = Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0))
            val d0 = direction(random.nextDouble(0.0, 2 * PI))
            val d1 = direction(random.nextDouble(0.0, 2 * PI))
            val shortest = dubinsPath(start, d0, end, d1, 40.0)
            // scale invariance
            val scaled = dubinsPath(start * 2.0, d0, end * 2.0, d1, 80.0)
            assertEquals(2 * shortest.length, scaled.length, 1E-7)
            assertEquals(shortest.word, scaled.word)
        }
    }

    @Test
    fun geometry() {
        val random = Random(3)
        repeat(50) {
            val path = dubinsPath(
                Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0)), direction(random.nextDouble(0.0, 6.0)),
                Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0)), direction(random.nextDouble(0.0, 6.0)),
                random.nextDouble(10.0, 80.0)
            )
            // position and direction by arc length agree with each other
            for (k in 1..19) {
                val s = path.length * k / 20.0
                val numeric = (path.position(s + 1E-6) - path.position(s - 1E-6)) / 2E-6
                assertTrue(numeric.distanceTo(path.direction(s)) < 1E-5)
            }
            // the contour is tangent-continuous, ends where the path does and is about as long
            val contour = path.contour()
            val segments = contour.segments
            for ((a, b) in segments.zipWithNext()) {
                assertTrue(a.end.distanceTo(b.start) < 1E-9)
                assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-7)
            }
            assertTrue(segments.last().end.distanceTo(path.end) < 1E-9)
            assertEquals(path.length, contour.length, path.length * 1E-3)
        }
    }

    @Test
    fun throughPoints() {
        val points = listOf(Vector2(0.0, 0.0), Vector2(200.0, 50.0), Vector2(100.0, 200.0), Vector2(300.0, 300.0))
        for (closed in listOf(false, true)) {
            val contour = points.dubinsPath(30.0, closed)
            assertEquals(closed, contour.closed)
            val joints = contour.segments.map { it.start } + contour.segments.last().end
            for (p in points) assertTrue(joints.any { it.distanceTo(p) < 1E-7 })
            for ((a, b) in contour.segments.zipWithNext()) assertTrue(a.direction(1.0).distanceTo(b.direction(0.0)) < 1E-7)
        }
    }
}
