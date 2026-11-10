import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.extra.textoncontour.PathBaseline3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPathBaseline3D {
    private fun Matrix44.x() = Vector3(c0r0, c0r1, c0r2)
    private fun Matrix44.y() = Vector3(c1r0, c1r1, c1r2)
    private fun Matrix44.z() = Vector3(c2r0, c2r1, c2r2)
    private fun Matrix44.o() = Vector3(c3r0, c3r1, c3r2)

    @Test
    fun alongAStraightPathTheTextStandsUpright() {
        val path = Path3D(listOf(Segment3D(Vector3.ZERO, Vector3(20.0, 0.0, 0.0))), false)
        val baseline = PathBaseline3D(path, Vector3.UNIT_Y)
        assertEquals(20.0, baseline.length, 1E-9)
        val pose = baseline.pose(5.0)
        // x along the path, y away from the ascenders (down), z = x × y facing -z
        assertTrue(pose.x().distanceTo(Vector3.UNIT_X) < 1E-9)
        assertTrue(pose.y().distanceTo(-Vector3.UNIT_Y) < 1E-9)
        assertTrue(pose.z().distanceTo(-Vector3.UNIT_Z) < 1E-9)
        assertTrue(pose.o().distanceTo(Vector3(5.0, 0.0, 0.0)) < 1E-6)
        // the offset moves the baseline towards the ascenders
        assertTrue(PathBaseline3D(path, Vector3.UNIT_Y, offset = 2.0).pose(5.0).o().distanceTo(Vector3(5.0, 2.0, 0.0)) < 1E-6)
    }

    @Test
    fun alongAHelixTheFramesFollowThePathWithoutJumps() {
        val path = List(33) { i -> Vector3(cos(i * PI / 8) * 6.0, i * 0.5, sin(i * PI / 8) * 6.0) }.catmullRom(0.5, false).toPath3D()
        val baseline = PathBaseline3D(path, Vector3.UNIT_Y)
        val poses = (0..400).map { baseline.pose(it / 400.0 * baseline.length) }
        for (p in poses) {
            assertEquals(1.0, p.x().length, 1E-9)
            assertEquals(1.0, p.y().length, 1E-9)
            assertEquals(0.0, p.x().dot(p.y()), 1E-9)
            assertTrue(p.x().cross(p.y()).distanceTo(p.z()) < 1E-9)
        }
        // x follows the path: the step to the next pose is along it
        for ((a, b) in poses.zipWithNext()) {
            assertTrue((b.o() - a.o()).normalized.dot(a.x()) > 0.99)
        }
        // the frames turn a little from step to step, they do not jump
        val turns = poses.zipWithNext { a, b -> acos(a.y().dot(b.y()).coerceIn(-1.0, 1.0)) }
        assertTrue(turns.max() < 0.1, "${turns.max()}")
    }
}
