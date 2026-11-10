import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPatchFrames {
    private fun Matrix44.x() = Vector3(c0r0, c0r1, c0r2)
    private fun Matrix44.y() = Vector3(c1r0, c1r1, c1r2)
    private fun Matrix44.z() = Vector3(c2r0, c2r1, c2r2)
    private fun Matrix44.o() = Vector3(c3r0, c3r1, c3r2)

    private fun assertOrthonormal(f: Matrix44, at: String) {
        listOf(f.x(), f.y(), f.z(), f.o()).forEach { v -> assertTrue(v.x.isFinite() && v.y.isFinite() && v.z.isFinite(), "NaN at $at") }
        assertEquals(1.0, f.x().length, 1E-9, at)
        assertEquals(1.0, f.y().length, 1E-9, at)
        assertEquals(1.0, f.z().length, 1E-9, at)
        assertEquals(0.0, f.x().dot(f.y()), 1E-9, at)
        assertEquals(0.0, f.x().dot(f.z()), 1E-9, at)
        assertTrue((f.x().cross(f.y())).distanceTo(f.z()) < 1E-9, "not right-handed at $at")
    }

    @Test
    fun framesFollowThePatch() {
        val patch = BezierPatch3D(List(4) { j -> List(4) { i ->
            val a = (j / 3.0 - 0.5) * PI / 2
            Vector3(i * 10.0 / 3.0 + j * 0.7, sin(a) * 6.0, cos(a) * 6.0 + i * 0.5)
        } })
        val random = Random(3)
        val uvs = List(50) { Vector2(random.nextDouble(), random.nextDouble()) } + listOf(Vector2.ZERO, Vector2(1.0, 1.0))
        for ((uv, f) in uvs.zip(patch.frames(uvs))) {
            assertOrthonormal(f, "$uv")
            assertTrue(f.o().distanceTo(patch.position(uv.x, uv.y)) < 1E-12)
            assertTrue(f.z().distanceTo(patch.normal(uv.x, uv.y)) < 1E-12)
            // x along du, y the way dv goes
            assertTrue(f.x().dot(patch.du(uv.x, uv.y).normalized) > 0.99)
            assertTrue(f.y().dot(patch.dv(uv.x, uv.y)) > 0.0)
        }
    }

    @Test
    fun aCollapsedEdgeStillHasFrames() {
        // the edge where v = 0 is a single point, du vanishes all along it
        val apex = Vector3(0.0, 0.0, 5.0)
        val patch = BezierPatch3D(List(4) { j -> List(4) { i ->
            if (j == 0) apex else Vector3(i * 3.0 - 4.5, j * 2.0, 5.0 - j * 1.5)
        } })
        assertEquals(0.0, patch.du(0.5, 0.0).length, 1E-12)
        for (i in 0..10) assertOrthonormal(patch.frame(i / 10.0, 0.0), "u ${i / 10.0}, v 0")
        assertOrthonormal(patch.frame(0.0, 0.0), "corner")
    }
}
