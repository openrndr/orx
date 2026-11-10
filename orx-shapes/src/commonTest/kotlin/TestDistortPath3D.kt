import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDistortPath3D {
    private val path = List(8) { i -> Vector3(cos(i * PI / 4) * 10, i * 1.5, sin(i * PI / 4) * 10) }
        .catmullRom(0.5, false).toPath3D()

    /** a twist around the y axis by an angle that grows with y */
    private fun twist(p: Vector3): Vector3 {
        val a = p.y * 0.8
        return Vector3(p.x * cos(a) - p.z * sin(a), p.y, p.x * sin(a) + p.z * cos(a))
    }

    /** the largest distance of [distorted] from the distortion of [original], at points of every segment */
    private fun deviation(original: Path3D, distorted: Path3D): Double {
        // the distorted path has more segments, its points are compared with the nearest distorted original point
        val reference = (0..4000).map { twist(original.position(it / 4000.0)) }
        return (0..800).maxOf { i ->
            val p = distorted.position(i / 800.0)
            reference.minOf { it.distanceTo(p) }
        }
    }

    @Test
    fun theDistortedPathFollowsTheDistortionWithinTheTolerance() {
        val coarse = path.distort(errorTolerance = 0.5, distort = ::twist)
        val fine = path.distort(errorTolerance = 0.01, distort = ::twist)
        assertTrue(fine.segments.size > coarse.segments.size, "${coarse.segments.size} ${fine.segments.size}")
        assertTrue(deviation(path, fine) < 0.05, "fine ${deviation(path, fine)}")
        assertTrue(deviation(path, coarse) < 1.5, "coarse ${deviation(path, coarse)}")
        // the ends are the distorted ends, and the pieces join exactly
        assertTrue(fine.segments.first().start.distanceTo(twist(path.segments.first().start)) < 1E-9)
        assertTrue(fine.segments.last().end.distanceTo(twist(path.segments.last().end)) < 1E-9)
        assertTrue(fine.segments.zipWithNext().all { (a, b) -> a.end.distanceTo(b.start) < 1E-9 })
    }

    @Test
    fun aClosedPathStaysClosed() {
        val ring = List(6) { i -> Vector3(cos(i * PI / 3) * 4, 0.0, sin(i * PI / 3) * 4) }.catmullRom(0.5, true).toPath3D()
        val moved = ring.distort { it + Vector3(0.0, 2.0, 0.0) }
        assertTrue(moved.closed)
        // a translation is fitted exactly, one piece per segment
        assertEquals(ring.segments.size, moved.segments.size)
        for (i in 0..50) assertTrue(moved.position(i / 50.0).distanceTo(ring.position(i / 50.0) + Vector3(0.0, 2.0, 0.0)) < 1E-6)
    }
}
