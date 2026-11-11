package rectify

import org.openrndr.extra.shapes.rectify.RectifiedPath
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toContour
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.EuclideanVector
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Path
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestRectifiedPathPrecision {

    /** the parameter of [path] at a length along it, from a dense table of chords: the reference */
    private class Reference<T : EuclideanVector<T>>(val path: Path<T>) {
        private val n = path.segments.size * 2048
        private val ts = DoubleArray(n + 1) { it.toDouble() / n }
        private val ss = DoubleArray(n + 1)

        init {
            var previous = path.position(0.0)
            for (i in 1..n) {
                val p = path.position(ts[i])
                ss[i] = ss[i - 1] + (p - previous).length
                previous = p
            }
        }

        val length = ss[n]

        fun parameterAt(s: Double): Double {
            if (s <= 0.0) return 0.0
            if (s >= length) return 1.0
            var lo = 0
            var hi = n
            while (hi - lo > 1) {
                val mid = (lo + hi) / 2
                if (ss[mid] <= s) lo = mid else hi = mid
            }
            return ts[lo] + (ts[hi] - ts[lo]) * (s - ss[lo]) / (ss[hi] - ss[lo])
        }
    }

    /** the largest distance of the positions of [rectified] from those at the same part of the length of its path */
    private fun <T : EuclideanVector<T>> maxError(rectified: RectifiedPath<T>): Double {
        val reference = Reference(rectified.originalPath)
        return (0..1000).maxOf { i ->
            val t = i / 1000.0
            val expected = rectified.originalPath.position(reference.parameterAt(t * reference.length))
            (rectified.position(t) - expected).length
        }
    }

    private val circle = Circle(0.0, 0.0, 100.0).contour

    private val catmull = Random(1).let { random ->
        List(48) { i ->
            Vector2(
                cos(i * 0.13) * 300 + random.nextDouble(-60.0, 60.0),
                sin(i * 0.13) * 200 + random.nextDouble(-60.0, 60.0)
            )
        }.catmullRom(0.5, true).toContour()
    }

    // a near-cusp, a line and a cubic with a control point next to its start: the speed changes a lot in a segment
    private val nearCusp = contour {
        moveTo(0.0, 0.0)
        curveTo(Vector2(300.0, 200.0), Vector2(0.0, 200.0), Vector2(300.0, 1.0))
        lineTo(400.0, 0.0)
        curveTo(Vector2(401.0, 1.0), Vector2(600.0, 300.0), Vector2(800.0, 0.0))
    }

    @Test
    fun positionsAreWithinTheLengthTolerance() {
        for (c in listOf(circle, catmull, nearCusp)) {
            val error = maxError(c.rectified())
            assertTrue(error < 2E-3, "error $error at the default tolerance")
        }
    }

    @Test
    fun aSmallerToleranceGivesSmallerErrors() {
        for (c in listOf(circle, catmull)) {
            val error = maxError(c.rectified(lengthTolerance = 1E-6))
            assertTrue(error < 1E-4, "error $error at tolerance 1E-6")
        }
    }

    @Test
    fun polylinesAreExact() {
        val polyline = ShapeContour.fromPoints(List(201) { i -> Vector2(i * 0.5, sin(i * 0.05) * 30) }, false)
        val error = maxError(polyline.rectified())
        assertTrue(error < 1E-8, "error $error on a polyline")
    }

    @Test
    fun path3DIsWithinTheLengthTolerance() {
        val helix = List(33) { i -> Vector3(cos(i * 0.4) * 6.0, i * 0.25, sin(i * 0.4) * 6.0) }
            .catmullRom(0.5, false).toPath3D()
        val error = maxError(helix.rectified())
        assertTrue(error < 2E-3, "error $error on a 3D path")
    }

    @Test
    fun inverseRectifyInvertsRectify() {
        for (c in listOf(circle, catmull, nearCusp)) {
            val r = c.rectified()
            for (i in 0..1000) {
                val t = i / 1000.0
                assertEquals(t, r.inverseRectify(r.rectify(t)), 1E-9)
                assertEquals(t, r.rectify(r.inverseRectify(t)), 1E-9)
            }
        }
    }

    @Test
    fun rectifyIsMonotonic() {
        for (c in listOf(circle, catmull, nearCusp)) {
            val r = c.rectified()
            var previous = 0.0
            for (i in 0..10000) {
                val u = r.rectify(i / 10000.0)
                assertTrue(u >= previous, "rectify decreases at ${i / 10000.0}")
                previous = u
            }
            assertEquals(0.0, r.rectify(0.0))
            assertEquals(1.0, r.rectify(1.0))
        }
    }

    @Test
    fun pointsCloseClosedPaths() {
        val points = circle.rectified().points
        assertEquals(circle.length.toInt() + 1, points.size)
        assertEquals(1.0, points.last().second)
        assertEquals(0.0, points.first().second)
        val open = nearCusp.rectified().points
        assertEquals(1.0, open.last().second, 1E-12)
    }
}
