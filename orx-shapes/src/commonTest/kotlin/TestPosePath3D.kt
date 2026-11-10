import org.openrndr.extra.shapes.pose.pose
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPosePath3D {
    private fun Matrix44.x() = Vector3(c0r0, c0r1, c0r2)
    private fun Matrix44.y() = Vector3(c1r0, c1r1, c1r2)
    private fun Matrix44.z() = Vector3(c2r0, c2r1, c2r2)
    private fun Matrix44.t() = Vector3(c3r0, c3r1, c3r2)

    /** the closed path of a camera ride: through 7 points, about 70 units long */
    private val ride = List(7) { i -> Vector3(cos(i * 2 * PI / 7) * 12, if (i % 2 == 0) 2.0 else -2.0, sin(i * 2 * PI / 7) * 9) }
        .catmullRom(0.5, true).toPath3D()

    @Test
    fun theFirstFrameIsUp() {
        val pose = ride.rectified().pose(Vector3.UNIT_Y).pose(0.0)
        val z = pose.z()
        val expected = (Vector3.UNIT_Y - z * (Vector3.UNIT_Y dot z)).normalized
        assertTrue(pose.y().distanceTo(expected) < 1E-9, "${pose.y()} $expected")
        // orthonormal and right-handed
        assertEquals(1.0, pose.x().length, 1E-9)
        assertEquals(0.0, pose.x() dot pose.y(), 1E-9)
        assertTrue((pose.x() cross pose.y()).distanceTo(z) < 1E-9)
    }

    @Test
    fun anOpenPlanarPathKeepsItsUpWithoutTwist() {
        // a path in the xz plane: transport keeps the up of every frame at y, a closure twist would not
        val path = List(6) { i -> Vector3(i * 3.0, 0.0, if (i % 2 == 0) 0.0 else 4.0) }.catmullRom(0.5, false).toPath3D()
        val poses = path.rectified().pose(Vector3.UNIT_Y)
        for (i in 0..100) {
            val y = poses.pose(i / 100.0).y()
            assertTrue(y.distanceTo(Vector3.UNIT_Y) < 1E-6, "at ${i / 100.0}: $y")
        }
    }

    @Test
    fun aRideMovesAndTurnsEvenly() {
        val rectified = ride.rectified()
        val poses = rectified.pose(Vector3.UNIT_Y)
        val length = ride.length
        // a step of 0.0357 units, as of a camera at 0.03 per second at 60 frames per second
        val dt = 0.0357 / length
        val steps = (1.0 / dt).toInt()
        val positions = List(steps + 1) { poses.pose(it * dt).t() }
        val distances = positions.zipWithNext { a, b -> a.distanceTo(b) }
        val mean = distances.average()
        assertTrue(distances.all { abs(it - mean) / mean < 0.002 }, "steps from ${distances.min()} to ${distances.max()}")
        // the turning per step changes smoothly: no jumps between neighbouring steps
        val directions = List(steps + 1) { poses.pose(it * dt).z() }
        val turns = directions.zipWithNext { a, b -> acos((a dot b).coerceIn(-1.0, 1.0)) }
        // the turning per step changes smoothly: its second differences, which a ripple would make large, are small
        // against its first differences; the knots of the spline, where its curvature jumps, are left out
        val changes = turns.zipWithNext { a, b -> b - a }
        val knots = ride.segments.size
        val jitter = changes.zipWithNext { a, b -> abs(b - a) }.withIndex()
            .filter { (i, _) ->
                // the parameter of the spline at the step, whose whole numbers are its knots
                val k = rectified.rectify((i + 1) * dt) * knots
                abs(k - kotlin.math.round(k)) > 0.02
            }
            .maxOf { it.value }
        println("POSE steps ${distances.min()}..${distances.max()} jitter $jitter of ${changes.maxOf { abs(it) }}")
        assertTrue(jitter < 0.1 * changes.maxOf { abs(it) }, "jitter $jitter, changes ${changes.maxOf { abs(it) }}")
        // the roll across the seam is like that elsewhere
        val ys = List(steps + 1) { poses.pose(it * dt).y() }
        val rolls = ys.zipWithNext { a, b -> acos((a dot b).coerceIn(-1.0, 1.0)) }
        val seam = acos((poses.pose(1.0 - 1E-9).y() dot poses.pose(0.0).y()).coerceIn(-1.0, 1.0))
        assertTrue(seam < rolls.max() + 1E-6, "seam $seam, most ${rolls.max()}")
    }

    @Test
    fun linearSegmentsPointTheWayThePathGoes() {
        val path = Path3D(listOf(Segment3D(Vector3.ZERO, Vector3(10.0, 0.0, 0.0)), Segment3D(Vector3(10.0, 0.0, 0.0), Vector3(10.0, 0.0, 10.0))), false)
        val poses = path.rectified().pose(Vector3.UNIT_Y)
        assertTrue(poses.pose(0.25).z().distanceTo(Vector3.UNIT_X) < 1E-6, "${poses.pose(0.25).z()}")
        assertTrue(poses.pose(0.75).z().distanceTo(Vector3.UNIT_Z) < 1E-6, "${poses.pose(0.75).z()}")
    }
}
