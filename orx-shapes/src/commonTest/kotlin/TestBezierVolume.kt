import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class TestBezierVolume {
    private val box = Box(Vector3(-6.0, -2.0, -2.0), 12.0, 4.0, 4.0)

    /** a volume twisted around the x axis and bent up */
    private val twisted = BezierVolume.fromBox(box).map { p ->
        val a = (p.x + 6.0) / 12.0 * 1.5
        Vector3(p.x, p.y * cos(a) - p.z * sin(a) + (p.x + 6.0) * (p.x + 6.0) * 0.04, p.y * sin(a) + p.z * cos(a))
    }

    @Test
    fun derivativesMatchFiniteDifferences() {
        val random = Random(4)
        val h = 1E-6
        repeat(20) {
            val u = random.nextDouble(0.01, 0.99)
            val v = random.nextDouble(0.01, 0.99)
            val w = random.nextDouble(0.01, 0.99)
            val du = (twisted.position(u + h, v, w) - twisted.position(u - h, v, w)) / (2 * h)
            val dv = (twisted.position(u, v + h, w) - twisted.position(u, v - h, w)) / (2 * h)
            val dw = (twisted.position(u, v, w + h) - twisted.position(u, v, w - h)) / (2 * h)
            assertTrue(twisted.du(u, v, w).distanceTo(du) < 1E-5)
            assertTrue(twisted.dv(u, v, w).distanceTo(dv) < 1E-5)
            assertTrue(twisted.dw(u, v, w).distanceTo(dw) < 1E-5)
        }
        // the volume of a box is stretched by its size, the same everywhere
        val j = BezierVolume.fromBox(box).jacobian(0.3, 0.6, 0.2)
        assertTrue(Vector3(j.c0r0, j.c0r1, j.c0r2).distanceTo(Vector3(12.0, 0.0, 0.0)) < 1E-9)
        assertTrue(Vector3(j.c1r0, j.c1r1, j.c1r2).distanceTo(Vector3(0.0, 4.0, 0.0)) < 1E-9)
        assertTrue(Vector3(j.c2r0, j.c2r1, j.c2r2).distanceTo(Vector3(0.0, 0.0, 4.0)) < 1E-9)
    }
}
