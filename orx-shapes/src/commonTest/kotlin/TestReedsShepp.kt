import org.openrndr.extra.shapes.dubins.dubinsPath
import org.openrndr.extra.shapes.reedsshepp.reedsSheppPath
import org.openrndr.extra.shapes.reedsshepp.reedsSheppPaths
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestReedsShepp {

    private fun direction(angle: Double) = Vector2(cos(angle), sin(angle))

    private class Pose(val p: Vector2, val d: Vector2)

    private fun randomPose(random: Random) =
        Pose(Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0)), direction(random.nextDouble(0.0, 2 * PI)))

    @Test
    fun everyCandidateReachesTheEndPose() {
        val random = Random(1)
        var candidates = 0
        repeat(500) {
            val a = randomPose(random)
            val b = randomPose(random)
            val radius = random.nextDouble(5.0, 150.0)
            val paths = reedsSheppPaths(a.p, a.d, b.p, b.d, radius)
            assertTrue(paths.isNotEmpty())
            for (path in paths) {
                assertTrue(path.end.distanceTo(b.p) < 1E-6 * (1.0 + radius), "${path.segments} misses the end point by ${path.end.distanceTo(b.p)}")
                assertTrue(path.endDirection.distanceTo(b.d) < 1E-8, "${path.segments} misses the end heading")
                assertTrue(path.length >= a.p.distanceTo(b.p) - 1E-9)
                candidates++
            }
        }
        assertTrue(candidates > 2000)
    }

    @Test
    fun neverLongerThanDubins() {
        val random = Random(2)
        repeat(300) {
            val a = randomPose(random)
            val b = randomPose(random)
            val radius = random.nextDouble(5.0, 150.0)
            val rs = reedsSheppPath(a.p, a.d, b.p, b.d, radius)
            val dubins = dubinsPath(a.p, a.d, b.p, b.d, radius)
            assertTrue(rs.length <= dubins.length + 1E-7, "${rs.length} > ${dubins.length}")
        }
        // far ahead in the same direction, driving forwards is optimal
        val ahead = reedsSheppPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(500.0, 30.0), Vector2(1.0, 0.0), 20.0)
        val forward = dubinsPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(500.0, 30.0), Vector2(1.0, 0.0), 20.0)
        assertEquals(forward.length, ahead.length, 1E-7)
        assertEquals(0, ahead.cusps)
    }

    @Test
    fun reversible() {
        // a path driven backwards goes from the end pose to the start pose, so both directions are equally long
        val random = Random(3)
        repeat(300) {
            val a = randomPose(random)
            val b = randomPose(random)
            val radius = random.nextDouble(5.0, 150.0)
            val there = reedsSheppPath(a.p, a.d, b.p, b.d, radius)
            val back = reedsSheppPath(b.p, b.d, a.p, a.d, radius)
            assertEquals(there.length, back.length, 1E-7 * (1.0 + there.length))
        }
    }

    @Test
    fun parallelParking() {
        // moving sideways by a little needs gear changes, and is far shorter than any forward-only path
        val r = 10.0
        val path = reedsSheppPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(0.0, 3.0), Vector2(1.0, 0.0), r)
        assertTrue(path.cusps >= 1)
        assertTrue(path.length < dubinsPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(0.0, 3.0), Vector2(1.0, 0.0), r).length / 2)
        // straight back
        val back = reedsSheppPath(Vector2.ZERO, Vector2(1.0, 0.0), Vector2(-50.0, 0.0), Vector2(1.0, 0.0), r)
        assertEquals(50.0, back.length, 1E-9)
        assertTrue(back.segments.all { it.length <= 0.0 })
        assertEquals(0.0, reedsSheppPath(Vector2(1.0, 2.0), Vector2(0.0, 1.0), Vector2(1.0, 2.0), Vector2(0.0, 1.0), r).length)
    }

    @Test
    fun geometry() {
        val random = Random(4)
        repeat(50) {
            val a = randomPose(random)
            val b = randomPose(random)
            val path = reedsSheppPath(a.p, a.d, b.p, b.d, random.nextDouble(10.0, 80.0))
            // the motion is the heading forwards and its opposite in reverse
            for (k in 1..39) {
                val s = path.length * k / 40.0
                val numeric = (path.position(s + 1E-6) - path.position(s - 1E-6)) / 2E-6
                val expected = path.direction(s) * (if (path.isForward(s)) 1.0 else -1.0)
                if (numeric.length > 0.5) assertTrue(numeric.distanceTo(expected) < 1E-4, "motion at $s")
            }
            // the contour is continuous and as long as the path; every gear part is tangent-continuous
            val contour = path.contour()
            assertTrue(contour.segments.last().end.distanceTo(b.p) < 1E-6)
            assertEquals(path.length, contour.length, path.length * 1E-3)
            val parts = path.gearContours()
            assertEquals(path.cusps + 1, parts.size)
            for ((part, _) in parts) for ((s1, s2) in part.segments.zipWithNext()) {
                assertTrue(s1.direction(1.0).distanceTo(s2.direction(0.0)) < 1E-7)
            }
        }
    }
}
