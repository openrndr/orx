import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class TestDistortRectifiedPath3D {
    private val path = List(8) { i -> Vector3(cos(i * PI / 4) * 30, i * 4.0, sin(i * PI / 4) * 30) }
        .catmullRom(0.5, false).toPath3D()
    private val rectified = path.rectified()

    /** a wave up and down of 2 units with a wavelength of 10 units of length along the path */
    private fun wave(t: Double, p: Vector3) = p + Vector3(0.0, sin(t * path.length / 10.0 * 2 * PI) * 2.0, 0.0)

    /** the largest distance of [distorted] from the wave applied along the rectified path */
    private fun deviation(distorted: Path3D): Double {
        val reference = (0..20000).map { wave(it / 20000.0, rectified.position(it / 20000.0)) }
        return (0..2000).maxOf { i ->
            val p = distorted.position(i / 2000.0)
            reference.minOf { it.distanceTo(p) }
        }
    }

    private fun checkPath(distorted: Path3D, tolerance: Double) {
        assertTrue(deviation(distorted) < tolerance, "deviation ${deviation(distorted)}")
        assertTrue(distorted.segments.first().start.distanceTo(wave(0.0, rectified.position(0.0))) < 1E-9)
        assertTrue(distorted.segments.last().end.distanceTo(wave(1.0, rectified.position(1.0))) < 1E-9)
        assertTrue(distorted.segments.zipWithNext().all { (a, b) -> a.end.distanceTo(b.start) < 1E-9 })
    }

    @Test
    fun aWaveOfConstantWavelengthAlongThePath() {
        checkPath(rectified.distort(errorTolerance = 0.02, distort = ::wave), 0.1)
        checkPath(rectified.distortUniform(errorTolerance = 0.02, sampleDistance = 0.5, distort = ::wave), 0.1)
    }

    @Test
    fun theUniformDistortionJoinsSmoothly() {
        val distorted = rectified.distortUniform(errorTolerance = 0.02, sampleDistance = 0.5, distort = ::wave)
        // the directions at every joint of pieces of the same segment agree: the control points follow the tangents
        val joints = distorted.segments.zipWithNext().map { (a, b) ->
            (a.end - a.control.last()).normalized.dot((b.control.first() - b.start).normalized)
        }
        assertTrue(joints.count { it < 0.999 } <= path.segments.size, "corners ${joints.count { it < 0.999 }} of ${joints.size}")
    }

    @Test
    fun aClosedPathStaysClosed() {
        val ring = List(6) { i -> Vector3(cos(i * PI / 3) * 10, 0.0, sin(i * PI / 3) * 10) }.catmullRom(0.5, true).toPath3D().rectified()
        assertTrue(ring.distort { _, p -> p * 1.5 }.closed)
        assertTrue(ring.distortUniform { _, p -> p * 1.5 }.closed)
    }
}
