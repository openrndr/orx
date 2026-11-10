package bezierpatches

import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBezierVolumeEvaluation {
    private val random = Random(5)
    private val box = Box(Vector3(-6.0, -1.0, -1.0), 12.0, 2.0, 2.0)

    /** a volume twisted along x, its control points jittered */
    private val volume = BezierVolume.fromBox(box).map { p ->
        val a = 2.5 * (p.x - box.corner.x) / box.width
        Vector3(p.x, p.y * cos(a) - p.z * sin(a), p.y * sin(a) + p.z * cos(a)) +
                Vector3(random.nextDouble(-0.2, 0.2), random.nextDouble(-0.2, 0.2), random.nextDouble(-0.2, 0.2))
    }

    private fun b(t: Double, k: Int): Double {
        val s = 1 - t
        return listOf(s * s * s, 3 * s * s * t, 3 * s * t * t, t * t * t)[k]
    }

    @Test
    fun positionsAreThoseOfTheDefinition() {
        repeat(200) {
            val u = random.nextDouble(); val v = random.nextDouble(); val w = random.nextDouble()
            var expected = Vector3.ZERO
            for (k in 0 until 4) for (j in 0 until 4) for (i in 0 until 4) {
                expected += volume.points[k][j][i] * (b(u, i) * b(v, j) * b(w, k))
            }
            assertTrue(volume.position(u, v, w).distanceTo(expected) < 1E-12)
        }
    }

    @Test
    fun positionAndJacobianAgreeWithTheirParts() {
        repeat(200) {
            val u = random.nextDouble(); val v = random.nextDouble(); val w = random.nextDouble()
            val (p, j) = volume.positionAndJacobian(u, v, w)
            assertTrue(p.distanceTo(volume.position(u, v, w)) < 1E-12)
            assertTrue(Vector3(j.c0r0, j.c0r1, j.c0r2).distanceTo(volume.du(u, v, w)) < 1E-12)
            assertTrue(Vector3(j.c1r0, j.c1r1, j.c1r2).distanceTo(volume.dv(u, v, w)) < 1E-12)
            assertTrue(Vector3(j.c2r0, j.c2r1, j.c2r2).distanceTo(volume.dw(u, v, w)) < 1E-12)
            val h = 1E-6
            val du = (volume.position(u + h, v, w) - volume.position(u - h, v, w)) / (2 * h)
            assertTrue(volume.du(u, v, w).distanceTo(du) < 1E-6 * du.length)
        }
    }

    @Test
    fun deformedNormalsAreThoseOfTheDeformedSurface() {
        // a small square in the plane of x and y around a point of the box, its normal z: the normal of its image is
        // the cross product of the images of its sides
        repeat(100) {
            val p = box.corner + Vector3(random.nextDouble() * 12.0, random.nextDouble() * 2.0, random.nextDouble() * 2.0)
            val h = 1E-5
            val dx = volume.deform(box, p + Vector3(h, 0.0, 0.0)) - volume.deform(box, p - Vector3(h, 0.0, 0.0))
            val dy = volume.deform(box, p + Vector3(0.0, h, 0.0)) - volume.deform(box, p - Vector3(0.0, h, 0.0))
            val expected = dx.cross(dy).normalized
            val (q, n) = volume.deform(box, p, Vector3.UNIT_Z)
            assertTrue(q.distanceTo(volume.deform(box, p)) < 1E-12)
            assertEquals(1.0, n.length, 1E-12)
            assertTrue(n.distanceTo(expected) < 1E-6, "$n $expected")
        }
    }

    @Test
    fun theVolumeOfABoxLeavesPointsAndNormalsAsTheyAre() {
        val identity = BezierVolume.fromBox(box)
        repeat(50) {
            val p = box.corner + Vector3(random.nextDouble() * 12.0, random.nextDouble() * 2.0, random.nextDouble() * 2.0)
            val normal = Vector3(random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0)).normalized
            val (q, n) = identity.deform(box, p, normal)
            assertTrue(q.distanceTo(p) < 1E-12)
            assertTrue(n.distanceTo(normal) < 1E-12)
        }
    }

    @Test
    fun aCollapsedVolumeKeepsNormals() {
        // every control point at one place: the Jacobian is singular
        val point = BezierVolume(List(4) { List(4) { List(4) { Vector3(1.0, 2.0, 3.0) } } })
        val (q, n) = point.deform(box, Vector3.ZERO, Vector3.UNIT_Y)
        assertTrue(q.distanceTo(Vector3(1.0, 2.0, 3.0)) < 1E-12)
        assertTrue(n.distanceTo(Vector3.UNIT_Y) < 1E-12)
    }
}
