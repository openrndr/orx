import org.openrndr.extra.turtle.Turtle
import org.openrndr.extra.turtle.arc
import org.openrndr.extra.turtle.clothoid
import org.openrndr.extra.turtle.curve
import org.openrndr.extra.turtle.turtle
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCurvatureTurtle {

    private fun assertClose(expected: Vector2, actual: Vector2, tolerance: Double, message: String = "") =
        assertTrue(expected.distanceTo(actual) < tolerance, "$message: expected $expected, got $actual")

    @Test
    fun arcTurnsLikeRotate() {
        // a quarter circle of radius 50 ends where turning and moving would put it, facing the rotated direction
        val r = 50.0
        val curved = Turtle(Vector2.ZERO).apply { arc(PI / 2 * r, 1.0 / r) }
        val rotated = Turtle(Vector2.ZERO).apply { rotate(90.0) }
        assertClose(rotated.direction, curved.direction, 1E-9, "direction")
        // the center of the arc lies r along the side the turtle turns to, and the arc ends r beyond it
        val center = Vector2.ZERO + rotated.direction * r
        assertClose(center + Vector2(1.0, 0.0) * r, curved.position, 1E-9, "end")
        for (sign in listOf(1.0, -1.0)) {
            val t = Turtle(Vector2.ZERO).apply { arc(PI / 2 * r, sign / r) }
            val expected = Turtle(Vector2.ZERO).apply { rotate(sign * 90.0) }.direction
            assertClose(expected, t.direction, 1E-9, "sign $sign")
        }
    }

    @Test
    fun fullCircle() {
        val r = 80.0
        val contours = turtle(Vector2(10.0, 20.0)) { arc(2 * PI * r, 1.0 / r) }
        val contour = contours.single()
        assertClose(Vector2(10.0, 20.0), contour.segments.last().end, 1E-8, "closes")
        // every drawn point lies on the circle
        val center = Vector2(10.0, 20.0) + Turtle(Vector2.ZERO).apply { rotate(90.0) }.direction * r
        for (k in 0..200) {
            assertEquals(r, contour.position(k / 200.0).distanceTo(center), r * 1E-6)
        }
        assertEquals(2 * PI * r, contour.length, 2 * PI * r * 1E-4)
    }

    /** The end point of the curve with curvature [kappa] over [length], by brute-force Euler summation. */
    private fun reference(length: Double, kappa: (Double) -> Double): Pair<Vector2, Double> {
        val n = 1_000_000
        val h = length / n
        var p = Vector2.ZERO
        var theta = 0.0
        for (i in 0 until n) {
            val s = (i + 0.5) * h
            val mid = theta + kappa((i + 0.25) * h) * h / 2
            p += Vector2(cos(mid), sin(mid)) * h
            theta += kappa(s) * h
        }
        return Pair(p, theta)
    }

    @Test
    fun clothoid() {
        val length = 300.0
        val k0 = -0.01
        val k1 = 0.03
        val t = Turtle(Vector2.ZERO).apply { clothoid(length, k0, k1) }
        val (expected, theta) = reference(length) { s -> k0 + (k1 - k0) * s / length }
        // the turtle's frame has y the way a positive rotation turns it: (0, -1) on screen for the initial orientation
        assertClose(Vector2(expected.x, -expected.y), t.position, 1E-3, "end point")
        assertEquals((k0 + k1) / 2 * length, theta, 1E-6)
        assertClose(Vector2(cos(theta), -sin(theta)), t.direction, 1E-9, "end direction")
    }

    @Test
    fun drawnCurvature() {
        // the drawn Béziers follow the prescribed curvature of a clothoid
        val length = 400.0
        val contour = turtle(Vector2.ZERO) { clothoid(length, 0.0, 0.05) }.single()
        for (k in 1..19) {
            val target = length * k / 20
            // find the parameter at that arc length, then compare the curvature of the Bézier there
            var lo = 0.0; var hi = 1.0
            repeat(60) { val m = (lo + hi) / 2; if (contour.sub(0.0, m).length < target) lo = m else hi = m }
            val (segment, t) = contour.segment((lo + hi) / 2)
            val d1 = contour.segments[segment].derivative(t)
            val d2 = contour.segments[segment].derivative2(t)
            val curvature = abs(d1.x * d2.y - d1.y * d2.x) / (d1.length * d1.length * d1.length)
            assertEquals(0.05 * target / length, curvature, 0.05 * 0.01, "curvature at $target")
        }
    }

    @Test
    fun combinesWithTheTurtle() {
        // zero curvature is a straight line; the pen and the stack work as for forward
        val straight = Turtle(Vector2.ZERO).apply { curve(100.0) { 0.0 } }
        assertClose(Vector2(100.0, 0.0), straight.position, 1E-9)
        val contours = turtle(Vector2.ZERO) {
            push()
            arc(100.0, 0.02)
            pop()
            penUp()
            arc(100.0, -0.02)
            penDown()
            forward(50.0)
        }
        val drawn = contours.filter { !it.empty }
        assertEquals(2, drawn.size)
        // a general curvature: a sine wave of curvature turns back to the start direction
        val wave = Turtle(Vector2.ZERO).apply { curve(200.0) { s -> 0.03 * sin(2 * PI * s / 200.0) } }
        assertClose(Vector2(1.0, 0.0), wave.direction, 1E-9, "wave direction")
    }
}
