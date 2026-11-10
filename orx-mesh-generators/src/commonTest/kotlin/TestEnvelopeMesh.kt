import org.openrndr.extra.meshgenerators.TriangleMeshBuilder
import org.openrndr.extra.meshgenerators.envelope
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class TestEnvelopeMesh {
    private val box = Box(Vector3(-6.0, -2.0, -2.0), 12.0, 4.0, 4.0)

    /** a grid of [n] by [n] cells in the plane of z = 0.5 inside the box, its normals along +z */
    private fun grid(n: Int = 60) = TriangleMeshBuilder().apply {
        fun p(i: Int, j: Int) = Vector3(-5.5 + 11.0 * i / n, -1.5 + 3.0 * j / n, 0.5)
        for (i in 0 until n) for (j in 0 until n) {
            for (q in listOf(p(i, j), p(i + 1, j), p(i + 1, j + 1), p(i, j), p(i + 1, j + 1), p(i, j + 1))) {
                write(q, Vector3.UNIT_Z, Vector2.ZERO)
            }
        }
    }

    @Test
    fun aVolumeOfTheBoxLeavesAMeshAsItIs() {
        val before = grid().data
        val after = grid().apply { envelope(box, BezierVolume.fromBox(box)) }.data
        for ((a, b) in before.zip(after)) {
            assertTrue(a.position.distanceTo(b.position) < 1E-9)
            assertTrue(a.normal.distanceTo(b.normal) < 1E-9)
        }
    }

    @Test
    fun normalsStayPerpendicularToTheDeformedSurface() {
        val twisted = BezierVolume.fromBox(box).map { p ->
            val a = (p.x + 6.0) / 12.0 * 1.5
            Vector3(p.x, p.y * cos(a) - p.z * sin(a) + (p.x + 6.0) * (p.x + 6.0) * 0.04, p.y * sin(a) + p.z * cos(a))
        }
        /** the largest angle between a vertex normal and the normal of its deformed triangle */
        fun worst(n: Int): Double {
            val data = grid(n).apply { envelope(box, twisted) }.data
            var worst = 0.0
            for (t in 0 until data.size / 3) {
                val (a, b, c) = (0..2).map { data[3 * t + it] }
                val geometric = (b.position - a.position).cross(c.position - a.position).normalized
                for (v in listOf(a, b, c)) {
                    assertTrue(v.normal.length in 0.999999..1.000001)
                    worst = maxOf(worst, acos(v.normal.dot(geometric).coerceIn(-1.0, 1.0)))
                }
            }
            return worst
        }
        // the flat triangles differ from the curved surface by an angle that halves as the triangles halve: the
        // normals are those of the surface
        val coarse = worst(40)
        val fine = worst(80)
        assertTrue(fine < 0.6 * coarse && fine < 1.5 * PI / 180.0, "${coarse * 180.0 / PI} ${fine * 180.0 / PI} degrees")
    }
}
