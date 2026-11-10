import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import org.openrndr.shape.Path3D
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEnvelopeWarpPath3D {
    /** a coil of radius 1 around the x axis, from x = 0 to x = 10 */
    private val coil = List(41) { i -> Vector3(i * 0.25, cos(i * PI / 4), sin(i * PI / 4)) }.catmullRom(0.5, false).toPath3D()

    @Test
    fun aVolumeFromABoxLeavesAPathAsItIs() {
        val box = Box(Vector3(-1.0, -2.0, -2.0), 12.0, 4.0, 4.0)
        val enveloped = coil.envelope(box, BezierVolume.fromBox(box), errorTolerance = 1E-4)
        for (i in 0..200) assertTrue(enveloped.position(i / 200.0).distanceTo(coil.position(i / 200.0)) < 1E-3)
    }

    @Test
    fun aBentVolumePlacesPointsWhereItMapsThem() {
        val box = Box(Vector3(-1.0, -2.0, -2.0), 12.0, 4.0, 4.0)
        // the far end of the volume, along x, raised: the coil bends up
        val bent = BezierVolume.fromBox(box).map { p -> p + Vector3(0.0, (p.x + 1.0) * (p.x + 1.0) * 0.05, 0.0) }
        val enveloped = coil.envelope(box, bent, errorTolerance = 1E-3)
        // the ends of the result are the mapped ends of the coil
        val start = coil.position(0.0)
        val uvw = (start - box.corner) / Vector3(box.width, box.height, box.depth)
        assertTrue(enveloped.segments.first().start.distanceTo(bent.position(uvw.x, uvw.y, uvw.z)) < 1E-9)
        assertTrue(enveloped.position(1.0).y > coil.position(1.0).y + 5.0)
    }

    @Test
    fun aPathLiesOnAPatchAndItsZIsAlongTheNormal() {
        // a patch bent like part of a cylinder around the x axis, over the rectangle 0..10 by 0..6
        val patch = BezierPatch3D(List(4) { j -> List(4) { i ->
            val a = (j / 3.0 - 0.5) * PI / 2
            Vector3(i * 10.0 / 3.0, sin(a) * 6.0, cos(a) * 6.0)
        } })
        val basis = Rectangle(0.0, 0.0, 10.0, 6.0)
        val flat = Path3D(listOf(Segment3D(Vector3(1.0, 1.0, 0.0), Vector3(4.0, 3.0, 0.0), Vector3(6.0, 5.0, 0.0), Vector3(9.0, 2.0, 0.0))), false)
        val laid = flat.envelope(basis, patch, errorTolerance = 1E-4)
        // every point of the patch at the u and v of a point of the flat path is on the result
        val dense = (0..5000).map { laid.position(it / 5000.0) }
        for (i in 0..100) {
            val p = flat.position(i / 100.0)
            val nearest = dense.minOf { it.distanceTo(patch.position(p.x / 10.0, p.y / 6.0)) }
            assertTrue(nearest < 0.01, "$nearest")
        }
        // raised by 0.5 in z it is 0.5 away from the surface along the normal
        val raised = Path3D(listOf(Segment3D(Vector3(5.0, 3.0, 0.5), Vector3(5.0, 3.0, 0.5))), false).envelope(basis, patch)
        val onSurface = patch.position(0.5, 0.5)
        assertEquals(0.5, raised.segments.first().start.distanceTo(onSurface), 1E-9)
        assertTrue((raised.segments.first().start - onSurface).normalized.distanceTo(patch.normal(0.5, 0.5)) < 1E-9)
        assertEquals(1.0, patch.normal(0.2, 0.7).length, 1E-12)
    }

    @Test
    fun theNearestPointOfAPath() {
        val path = List(8) { i -> Vector3(cos(i * PI / 4) * 10, i * 2.0, sin(i * PI / 4) * 10) }.catmullRom(0.5, false).toPath3D().rectified()
        val random = Random(1)
        repeat(20) {
            val q = Vector3(random.nextDouble(-12.0, 12.0), random.nextDouble(-2.0, 16.0), random.nextDouble(-12.0, 12.0))
            val t = path.nearest(q)
            val brute = (0..20000).minOf { path.position(it / 20000.0).distanceTo(q) }
            assertTrue(path.position(t).distanceTo(q) <= brute + 1E-6, "${path.position(t).distanceTo(q)} > $brute")
        }
    }

    @Test
    fun aCoilFollowsTheWarpOfItsBasis() {
        val basis = Path3D(listOf(Segment3D(Vector3(0.0, 0.0, 0.0), Vector3(10.0, 0.0, 0.0))), false).rectified()
        val coilR = coil.rectified()
        // the same basis: nothing changes, every point of the result is on the coil
        val same = coilR.warp(basis, basis, errorTolerance = 1E-3)
        val reference = (0..8000).map { coil.position(it / 8000.0) }
        for (i in 0..100) {
            val p = same.position(i / 100.0)
            assertTrue(reference.minOf { it.distanceTo(p) } < 0.01, "at ${i / 100.0}")
        }
        // a basis moved and turned to run along z: the coil winds around the new line with its radius
        val turned = Path3D(listOf(Segment3D(Vector3(5.0, 5.0, 0.0), Vector3(5.0, 5.0, 10.0))), false).rectified()
        val warped = coilR.warp(basis, turned, up = Vector3.UNIT_Y, errorTolerance = 1E-3)
        // the frames along both lines have y up: along x the frame's x is -z, along z it is +x, so a point (x, y, z) of
        // the coil goes to (5 - z, 5 + y, x), and every point of the result maps back onto the coil
        for (i in 0..100) {
            val p = warped.position(i / 100.0)
            val back = Vector3(p.z, p.y - 5.0, 5.0 - p.x)
            assertTrue(reference.minOf { it.distanceTo(back) } < 0.01, "at ${i / 100.0}")
        }
        // and goes along the new line from end to end as it went along the old one
        assertTrue(warped.segments.first().start.distanceTo(Vector3(5.0, 5.0, 0.0)) in 0.99..1.01)
        assertEquals(0.0, warped.segments.first().start.z, 1E-6)
        assertEquals(10.0, warped.segments.last().end.z, 1E-6)
    }
}
