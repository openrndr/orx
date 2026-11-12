package distort

import org.openrndr.extra.shapes.bezierpatches.BezierPatch
import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.shape.Box
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Path3D
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDistortSmooth {
    private fun degrees(a: Vector2, b: Vector2) = acos((a.normalized dot b.normalized).coerceIn(-1.0, 1.0)) * 180 / PI
    private fun degrees(a: Vector3, b: Vector3) = acos((a.normalized dot b.normalized).coerceIn(-1.0, 1.0)) * 180 / PI

    /** the angles between the tangents where the segments of [c] meet */
    private fun joins(c: ShapeContour): List<Double> {
        val s = c.segments
        val range = if (c.closed) s.indices else 0 until s.size - 1
        return range.map { i -> degrees(s[i].derivative(1.0), s[(i + 1) % s.size].derivative(0.0)) }
    }

    private fun joins(c: Path3D): List<Double> {
        val s = c.segments
        val range = if (c.closed) s.indices else 0 until s.size - 1
        return range.map { i -> degrees(s[i].derivative(1.0), s[(i + 1) % s.size].derivative(0.0)) }
    }

    private val blob = hobbyCurve(List(9) { k ->
        val a = k * 2 * PI / 9
        Vector2(cos(a), sin(a)) * (80.0 + 25.0 * sin(3 * a)) + Vector2(150.0, 150.0)
    }, closed = true)

    private val helix = List(25) { k -> Vector3(k * 2.0, cos(k * 0.6) * 4.0, sin(k * 0.6) * 4.0) }
        .catmullRom(0.5, false).toPath3D()

    private fun swirl(p: Vector2): Vector2 {
        val q = p - Vector2(150.0, 150.0)
        val a = 0.004 * q.length
        return Vector2(q.x * cos(a) - q.y * sin(a), q.x * sin(a) + q.y * cos(a)) + Vector2(150.0, 150.0)
    }

    @Test
    fun smoothDistortionsOfSmoothCurvesJoinSmoothly() {
        val r = blob.rectified()
        val wave = { t: Double, p: Vector2 -> p + r.normal(t) * (8.0 * sin(t * 2 * PI * 12)) }
        val contours = listOf(
            "RectifiedContour.distort" to r.distort(errorTolerance = 0.05, distort = wave),
            "ShapeContour.distort" to blob.distort(errorTolerance = 0.05, distort = ::swirl),
            "ShapeContour.envelope" to blob.envelope(
                Rectangle(0.0, 0.0, 300.0, 300.0),
                BezierPatch(List(4) { j -> List(4) { i -> Vector2(i * 120.0 + 60.0 * sin(j * 1.3), j * 110.0 + 50.0 * cos(i * 1.7)) } })
            ),
            "RectifiedContour.warp" to r.warp(
                LineSegment(0.0, 150.0, 300.0, 150.0).contour.rectified(),
                hobbyCurve(List(7) { k -> Vector2(k * 50.0, 150.0 + 60.0 * sin(k * 1.1)) }).rectified()
            )
        )
        for ((name, c) in contours) {
            assertTrue(joins(c).max() < 0.1, "$name: joins up to ${joins(c).max()} degrees")
        }
        val twist = { p: Vector3 ->
            val a = 0.05 * p.x
            Vector3(p.x, p.y * cos(a) - p.z * sin(a), p.y * sin(a) + p.z * cos(a))
        }
        val box = Box(Vector3(0.0, -5.0, -5.0), 48.0, 10.0, 10.0)
        val volume = BezierVolume.fromBox(box).map(twist)
        val patch = BezierPatch3D(List(4) { j -> List(4) { i -> Vector3(i * 20.0, j * 5.0 - 7.5, 8.0 * sin(i * 1.1 + j * 0.7)) } })
        val rh = helix.rectified()
        val paths = listOf(
            "Path3D.distort" to helix.distort(distort = twist),
            "RectifiedPath3D.distort" to rh.distort { t, p -> twist(p) + Vector3(0.0, 0.0, 2.0 * sin(t * 2 * PI * 6)) },
            "Path3D.envelope (volume)" to helix.envelope(box, volume),
            "Path3D.envelope (patch)" to helix.envelope(Rectangle(0.0, -7.5, 60.0, 15.0), patch),
            "RectifiedPath3D.warp" to rh.warp(
                Path3D.fromPoints(listOf(Vector3.ZERO, Vector3(48.0, 0.0, 0.0)), false).rectified(),
                List(7) { k -> Vector3(k * 8.0, 10.0 * sin(k * 0.8), 6.0 * cos(k * 0.5)) }.catmullRom(0.5, false).toPath3D().rectified()
            )
        )
        for ((name, c) in paths) {
            assertTrue(joins(c).max() < 0.1, "$name: joins up to ${joins(c).max()} degrees")
        }
    }

    @Test
    fun cornersAreKept() {
        val square = Rectangle(60.0, 60.0, 180.0, 180.0).contour
        val distorted = square.distort(errorTolerance = 0.05, distort = ::swirl)
        val angles = joins(distorted)
        // four corners, every other join smooth
        assertEquals(4, angles.count { it > 45.0 })
        assertTrue(angles.filter { it <= 45.0 }.all { it < 0.1 }, "joins up to ${angles.filter { it <= 45.0 }.max()}")
    }

    @Test
    fun theErrorIsThatOfTheTolerance() {
        val r = blob.rectified()
        val wave = { t: Double, p: Vector2 -> p + r.normal(t) * (8.0 * sin(t * 2 * PI * 12)) }
        for (tolerance in listOf(0.5, 0.05)) {
            val c = r.distort(errorTolerance = tolerance, distort = wave)
            val worst = (0 until 2000).maxOf { k ->
                val t = k / 2000.0
                val exact = wave(t, r.position(t))
                c.nearest(exact).position.distanceTo(exact)
            }
            assertTrue(worst < 1.5 * tolerance, "off by up to $worst at tolerance $tolerance")
        }
    }

    @Test
    fun polarityIsKept() {
        val ccw = ShapeContour(blob.segments, true, YPolarity.CCW_POSITIVE_Y)
        assertEquals(YPolarity.CCW_POSITIVE_Y, ccw.distort(distort = ::swirl).polarity)
        assertEquals(YPolarity.CCW_POSITIVE_Y, ccw.rectified().distort { _, p -> swirl(p) }.polarity)
        assertEquals(YPolarity.CCW_POSITIVE_Y, ccw.rectified().distortUniform { _, p -> swirl(p) }.polarity)
    }

    @Test
    fun pointsAreDisplacedOnce() {
        val r = blob.rectified()
        var calls = 0
        val c = r.distort(errorTolerance = 0.05) { t, p -> calls++; p + r.normal(t) * (8.0 * sin(t * 2 * PI * 12)) }
        // fitting every piece, and every piece it was split from, from its own 25 points takes about
        // (2 x pieces - segments) x 25 calls; sharing the points of the grid takes well under that
        val separately = (2 * c.segments.size - blob.segments.size) * 25
        assertTrue(calls < 0.7 * separately, "$calls calls for ${c.segments.size} pieces, $separately when fitted separately")
    }

    @Test
    fun anUnchangedCurveIsReproduced() {
        // cubics with long control arms and loops, whose handles are longer than their chords
        val random = kotlin.random.Random(1)
        repeat(100) {
            fun point() = Vector2(random.nextDouble(0.0, 500.0), random.nextDouble(0.0, 500.0))
            val s = org.openrndr.shape.Segment2D(point(), point(), point(), point())
            val pieces = s.distort(0.5, 6, 16) { q, _ -> q }
            assertEquals(1, pieces.size, "an unchanged cubic is split into ${pieces.size} pieces")
            for (k in 0..50) {
                val t = k / 50.0
                assertTrue(pieces[0].position(t).distanceTo(s.position(t)) < 1E-6)
            }
        }
    }
}
