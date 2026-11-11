package frames

import org.openrndr.extra.shapes.frames.frames
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFrames {
    private fun Matrix44.x() = Vector3(c0r0, c0r1, c0r2)
    private fun Matrix44.y() = Vector3(c1r0, c1r1, c1r2)
    private fun Matrix44.z() = Vector3(c2r0, c2r1, c2r2)
    private fun Matrix44.t() = Vector3(c3r0, c3r1, c3r2)

    private val helix = List(33) { i -> Vector3(cos(i * 0.4) * 6.0, i * 0.25, sin(i * 0.4) * 6.0) }
        .catmullRom(0.5, false).toPath3D().rectified()

    private val trefoil = List(64) { i ->
        val a = i * 2 * PI / 64
        Vector3(sin(a) + 2 * sin(2 * a), cos(a) - 2 * cos(2 * a), -sin(3 * a)) * 3.0
    }.catmullRom(0.5, true).toPath3D().rectified()

    /** the largest twist, in degrees, about the direction of the reference between [frames] and [reference] */
    private fun maxTwist(frames: List<Matrix44>, reference: List<Matrix44>): Double = frames.indices.maxOf { i ->
        val z = reference[i].z()
        val a = (frames[i].y() - z * (frames[i].y() dot z)).normalized
        val b = reference[i].y()
        abs(atan2((a cross b) dot z, a dot b)) * 180 / PI
    }

    private fun assertFrame(m: Matrix44) {
        assertEquals(1.0, m.x().length, 1E-9)
        assertEquals(1.0, m.y().length, 1E-9)
        assertEquals(1.0, m.z().length, 1E-9)
        assertEquals(0.0, m.x() dot m.y(), 1E-9)
        assertEquals(0.0, m.y() dot m.z(), 1E-9)
        assertTrue((m.x() cross m.y()).distanceTo(m.z()) < 1E-9, "not right-handed")
    }

    @Test
    fun aFrameAtEveryPosition() {
        for (n in listOf(1, 2, 3, 10)) {
            val positions = List(n) { Vector3(it.toDouble(), sin(it * 0.5), 2.0) }
            val frames = positions.frames(Vector3.UNIT_Y)
            assertEquals(n, frames.size)
            for ((p, f) in positions.zip(frames)) {
                assertTrue(f.t().distanceTo(p) < 1E-12, "frame at ${f.t()}, not at $p")
                assertFrame(f)
            }
        }
    }

    @Test
    fun theFirstFrameIsUp() {
        val ts = List(50) { it / 49.0 }
        val frames = helix.frames(ts, Vector3.UNIT_Y)
        val z = frames[0].z()
        val expected = (Vector3.UNIT_Y - z * (Vector3.UNIT_Y dot z)).normalized
        assertTrue(frames[0].y().distanceTo(expected) < 1E-9)
        assertTrue(frames[0].z().distanceTo(helix.direction(0.0).normalized) < 1E-9)
    }

    @Test
    fun framesAreRotationMinimizing() {
        val poses = PosePath3D(helix, Vector3.UNIT_Y)
        val ts = List(200) { it / 199.0 }
        val reference = ts.map { poses.pose(it) }
        // with the directions of the path
        val exact = helix.frames(ts, Vector3.UNIT_Y, analyticalDirections = true)
        assertTrue(maxTwist(exact, reference) < 1E-2, "twist ${maxTwist(exact, reference)} degrees")
        exact.forEach(::assertFrame)
        // with the directions estimated from the positions, more of them
        val ts800 = List(800) { it / 799.0 }
        val estimated = helix.frames(ts800, Vector3.UNIT_Y, analyticalDirections = false)
        val reference800 = ts800.map { poses.pose(it) }
        assertTrue(maxTwist(estimated, reference800) < 2E-2, "twist ${maxTwist(estimated, reference800)} degrees")
        val directionError = estimated.indices.maxOf { i ->
            kotlin.math.acos((estimated[i].z() dot reference800[i].z()).coerceIn(-1.0, 1.0)) * 180 / PI
        }
        assertTrue(directionError < 0.5, "directions up to $directionError degrees off")
    }

    @Test
    fun aPlanarPathKeepsItsUp() {
        val positions = List(40) { i -> Vector3(i * 0.5, 0.0, sin(i * 0.3) * 3.0) }
        for (f in positions.frames(Vector3.UNIT_Y)) {
            assertTrue(f.y().distanceTo(Vector3.UNIT_Y) < 1E-9, "up turned to ${f.y()}")
        }
    }

    @Test
    fun closedFramesMeetAtTheStart() {
        val poses = PosePath3D(trefoil, Vector3.UNIT_Y)
        // the last position the first again
        val ts = List(401) { it / 400.0 }
        val frames = trefoil.frames(ts, Vector3.UNIT_Y)
        assertTrue(frames.first().y().distanceTo(frames.last().y()) < 1E-9, "${frames.first().y()} ${frames.last().y()}")
        assertTrue(maxTwist(frames, ts.map { poses.pose(it) }) < 2E-2, "twist ${maxTwist(frames, ts.map { poses.pose(it) })}")
        // going on from the last position to the first
        val open = List(400) { it / 400.0 }
        val lap = trefoil.frames(open, Vector3.UNIT_Y)
        assertTrue(maxTwist(lap, open.map { poses.pose(it) }) < 2E-2, "twist ${maxTwist(lap, open.map { poses.pose(it) })}")
    }

    @Test
    fun awkwardPositionsGiveFrames() {
        val cases = listOf(
            // starting along the up
            List(4) { Vector3(it * it * 0.1, it.toDouble(), 0.0) },
            List(4) { Vector3(0.0, it.toDouble(), 0.0) },
            // a position twice
            listOf(Vector3.ZERO, Vector3(1.0, 0.0, 0.0), Vector3(1.0, 0.0, 0.0), Vector3(2.0, 1.0, 0.0)),
            // turning back
            listOf(Vector3.ZERO, Vector3(1.0, 0.0, 0.0), Vector3(0.0, 0.0, 0.0001), Vector3(-1.0, 0.0, 0.0)),
            // all at one place
            List(3) { Vector3(1.0, 2.0, 3.0) }
        )
        for (positions in cases) {
            val frames = positions.frames(Vector3.UNIT_Y)
            assertEquals(positions.size, frames.size)
            frames.forEach(::assertFrame)
        }
    }
}
