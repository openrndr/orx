package pose

import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class TestPosePath3DPrecision {
    private fun Matrix44.x() = Vector3(c0r0, c0r1, c0r2)
    private fun Matrix44.y() = Vector3(c1r0, c1r1, c1r2)
    private fun Matrix44.z() = Vector3(c2r0, c2r1, c2r2)
    private fun Matrix44.t() = Vector3(c3r0, c3r1, c3r2)

    /** the angle about [t] from [a] to [b] */
    private fun angle(a: Vector3, b: Vector3, t: Vector3): Double {
        val ap = (a - t * (a dot t)).normalized
        val bp = (b - t * (b dot t)).normalized
        return atan2((ap cross bp) dot t, (ap dot bp).coerceIn(-1.0, 1.0))
    }

    private fun transport(x: Vector3, t0: Vector3, t1: Vector3): Vector3 {
        val b = (t0 + t1).normalized
        return x - b * (2.0 * (x dot b))
    }

    /**
     * the x axes of the rotation minimizing frames of [path] at [n] + 1 equal steps of the rectified parameter, by
     * many small turns: the reference; the twist of a closed path is spread over its lap
     */
    private class Reference(val path: RectifiedPath3D, val n: Int, test: TestPosePath3DPrecision) {
        val tangents = Array(n + 1) { path.direction(it.toDouble() / n).normalized }
        var closure = 0.0
        val xs = arrayOfNulls<Vector3>(n + 1).also { x ->
            val z = tangents[0]
            x[0] = (Vector3.UNIT_Y - z * (Vector3.UNIT_Y dot z)).normalized cross z
            for (i in 1..n) x[i] = test.transport(x[i - 1]!!, tangents[i - 1], tangents[i])
            if (path.path.closed) {
                closure = test.angle(x[0]!!, x[n]!!, tangents[0])
                for (i in 0..n) {
                    val a = -closure * i / n
                    x[i] = x[i]!! * cos(a) + (tangents[i] cross x[i]!!) * sin(a)
                }
            }
        }

        fun x(t: Double, test: TestPosePath3DPrecision): Vector3 {
            val i = (t * n).toInt().coerceIn(0, n)
            val d = path.direction(t).normalized
            // with the share of the spread twist from the frame to t
            val x = test.transport(xs[i]!!, tangents[i], d)
            val a = -closure * (t - i.toDouble() / n)
            return x * cos(a) + (d cross x) * sin(a)
        }
    }

    /** the largest angle about the direction of the path between the frames of [poses] and the reference */
    private fun maxTwistError(poses: PosePath3D): Double {
        val reference = Reference(poses.path, 1 shl 16, this)
        return (0..1000).maxOf { i ->
            val t = i / 1000.0
            val pose = poses.pose(t)
            abs(angle(reference.x(t, this), pose.x(), pose.z()))
        }
    }

    private val helix = List(33) { i -> Vector3(cos(i * 0.4) * 6.0, i * 0.25, sin(i * 0.4) * 6.0) }
        .catmullRom(0.5, false).toPath3D()

    private val trefoil = List(64) { i ->
        val a = i * 2 * PI / 64
        Vector3(sin(a) + 2 * sin(2 * a), cos(a) - 2 * cos(2 * a), -sin(3 * a)) * 3.0
    }.catmullRom(0.5, true).toPath3D()

    // through random points: tight bends, where the direction turns by tens of degrees within an even step
    private val random = Random(3).let { r ->
        List(13) { Vector3(r.nextDouble(-5.0, 5.0), r.nextDouble(-5.0, 5.0), r.nextDouble(-5.0, 5.0)) }
            .catmullRom(0.5, false).toPath3D()
    }

    @Test
    fun framesAreRotationMinimizing() {
        for ((path, tolerance) in listOf(helix to 1E-5, trefoil to 1E-5, random to 5E-4)) {
            val error = maxTwistError(path.rectified().let { PosePath3D(it) })
            assertTrue(error < tolerance, "twist error $error")
        }
    }

    @Test
    fun framesAreAddedWhereThePathTurns() {
        val poses = PosePath3D(random.rectified())
        val turns = poses.frames.zipWithNext { a, b -> acos((a[2] dot b[2]).coerceIn(-1.0, 1.0)) }
        assertTrue(turns.max() <= PI / 180 + 1E-9, "turns up to ${turns.max()}")
        assertTrue(poses.frames.size > 64 * random.segments.size, "${poses.frames.size} frames")
    }

    @Test
    fun poseDerivativeIsTheChangeOfThePose() {
        for (path in listOf(helix, trefoil, random)) {
            val poses = PosePath3D(path.rectified())
            val h = 1E-5
            for (i in 1 until 100) {
                val t = i / 100.0
                // not at the knots of the spline, where its curvature jumps
                val k = poses.path.rectify(t) * path.segments.size
                if (abs(k - kotlin.math.round(k)) < 0.01) continue
                val expected = (poses.pose(t + h) - poses.pose(t - h)) / (2 * h)
                val actual = poses.poseDerivative(t)
                val scale = listOf(expected.x(), expected.y(), expected.z()).maxOf { it.length }
                val difference = expected - actual
                val error = listOf(difference.x(), difference.y(), difference.z()).maxOf { it.length }
                assertTrue(error < 1E-3 * scale, "turn at $t: $error of $scale")
                // the translation moves at the length of the path, up to the pace of the rectification
                val pace = difference.t().length / path.length
                assertTrue(pace < 1E-2, "speed at $t off by $pace")
            }
        }
    }

    @Test
    fun poseDerivativeAtTheEndsOfAnOpenPath() {
        val poses = PosePath3D(helix.rectified())
        val h = 1E-5
        for ((t, expected) in listOf(
            0.0 to (poses.pose(h) - poses.pose(0.0)) / h,
            1.0 to (poses.pose(1.0) - poses.pose(1.0 - h)) / h
        )) {
            val actual = poses.poseDerivative(t)
            val difference = expected - actual
            val scale = listOf(expected.x(), expected.y(), expected.z(), expected.t()).maxOf { it.length }
            val error = listOf(difference.x(), difference.y(), difference.z(), difference.t()).maxOf { it.length }
            assertTrue(error < 1E-2 * scale, "at $t: $error of $scale")
        }
    }
}
