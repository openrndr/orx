import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.smoothnormal.smoothNormalAngular
import org.openrndr.extra.shapes.smoothnormal.smoothNormalLinear
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSmoothNormal {
    private fun assertNear(expected: Vector2, actual: Vector2, eps: Double = 1E-3) {
        assertTrue(expected.distanceTo(actual) < eps, "expected $expected, got $actual")
    }

    @Test
    fun cornersOfSquareAreAveraged() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        // corners at t = 0.0, 0.25, 0.5, 0.75
        for (i in 0 until 4) {
            val t = i / 4.0
            val n = r.smoothNormalLinear(t, 10.0)
            val before = r.normal(t - 0.1)
            val after = r.normal(t + 0.1)
            assertNear((before + after).normalized, n)
        }
    }

    @Test
    fun outsideRadiusEqualsNormal() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        for (t in listOf(0.05, 0.125, 0.2, 0.3, 0.6, 0.95)) {
            assertNear(r.normal(t), r.smoothNormalLinear(t, 10.0))
        }
    }

    @Test
    fun halfwayInsideRadius() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        // 5 units before the corner at t = 0.25 (arc length 100): 3/4 own, 1/4 next
        val t = 95.0 / 400.0
        val n0 = r.normal(0.125)
        val n1 = r.normal(0.375)
        assertNear((n0 * 0.75 + n1 * 0.25).normalized, r.smoothNormalLinear(t, 10.0))
    }

    @Test
    fun openContourEndsAreNotBlended() {
        val c = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0)), closed = false)
        val r = c.rectified()
        assertNear(r.normal(0.0), r.smoothNormalLinear(0.0, 10.0))
        assertNear(r.normal(1.0), r.smoothNormalLinear(1.0, 10.0))
        val corner = r.smoothNormalLinear(0.5, 10.0)
        assertNear((r.normal(0.25) + r.normal(0.75)).normalized, corner)
        assertTrue(kotlin.math.abs(corner.length - 1.0) < 1E-9)
    }

    @Test
    fun angularCornersOfSquareAreBisected() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        for (i in 0 until 4) {
            val t = i / 4.0
            assertNear((r.normal(t - 0.1) + r.normal(t + 0.1)).normalized, r.smoothNormalAngular(t, 10.0))
        }
    }

    @Test
    fun angularOutsideRadiusEqualsNormal() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        for (t in listOf(0.05, 0.125, 0.2, 0.3, 0.6, 0.95)) {
            assertNear(r.normal(t), r.smoothNormalAngular(t, 10.0))
        }
    }

    @Test
    fun angularUsesSmoothstepWeight() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        // 5 units before the corner at t = 0.25: w = 0.25, smoothstep(0.25) = 0.15625 of a 90 degree turn
        val t = 95.0 / 400.0
        val n0 = r.normal(0.125)
        val n1 = r.normal(0.375)
        val sign = if (n0.x * n1.y - n0.y * n1.x < 0.0) -1.0 else 1.0
        val phi = sign * 0.15625 * PI / 2.0
        val expected = Vector2(n0.x * cos(phi) - n0.y * sin(phi), n0.x * sin(phi) + n0.y * cos(phi))
        assertNear(expected, r.smoothNormalAngular(t, 10.0))
    }

    @Test
    fun angularIsContinuousAroundCorners() {
        val r = Rectangle(0.0, 0.0, 100.0, 100.0).contour.rectified()
        val n = 4000
        var maxStep = 0.0
        for (i in 0 until n) {
            val a = r.smoothNormalAngular(i / n.toDouble(), 10.0)
            val b = r.smoothNormalAngular((i + 1) / n.toDouble(), 10.0)
            maxStep = maxOf(maxStep, acos(a.dot(b).coerceIn(-1.0, 1.0)))
        }
        // arc length step is 0.1, the steepest smoothstep rotation is 1.5 * 90 degrees / 20 units per unit
        assertTrue(maxStep < 0.02, "max step $maxStep")
    }

    @Test
    fun angularCuspRotatesAroundTip() {
        val c = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(0.0, 0.0)), closed = false)
        val r = c.rectified()
        val tip = r.smoothNormalAngular(0.5, 10.0)
        assertNear(Vector2(1.0, 0.0), tip)
        assertTrue(abs(tip.length - 1.0) < 1E-9)
    }
}
