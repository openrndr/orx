import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.PatchClip
import org.openrndr.extra.shapes.bezierpatches.PatchVertex
import org.openrndr.extra.shapes.bezierpatches.clip
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.path3d.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPatchClip {
    private val basis = Rectangle(0.0, 0.0, 100.0, 60.0)

    /** a patch bent like part of a cylinder around the x axis */
    private val curved = BezierPatch3D(List(4) { j -> List(4) { i ->
        val a = (j / 3.0 - 0.5) * PI / 2
        Vector3(i * 10.0 / 3.0, sin(a) * 6.0, cos(a) * 6.0)
    } })

    /** a ring: a disk with a hole, the hole wound the other way */
    private val ring = Shape(listOf(Circle(50.0, 30.0, 25.0).contour, Circle(50.0, 30.0, 10.0).contour.reversed))

    private fun edgeUse(triangles: List<PatchVertex>): Map<Pair<List<Long>, List<Long>>, Int> {
        fun key(p: Vector3) = listOf(round(p.x * 1E6).toLong(), round(p.y * 1E6).toLong(), round(p.z * 1E6).toLong())
        val count = mutableMapOf<Pair<List<Long>, List<Long>>, Int>()
        for (t in 0 until triangles.size / 3) {
            val c = (0..2).map { key(triangles[3 * t + it].position) }
            for (i in 0..2) {
                val a = c[i]
                val b = c[(i + 1) % 3]
                if (a == b) continue
                val e = if (a.toString() < b.toString()) a to b else b to a
                count[e] = (count[e] ?: 0) + 1
            }
        }
        return count
    }

    @Test
    fun aThickRingIsAClosedSolid() {
        for (thickness in listOf(0.8, -0.8)) {
            val solid = curved.clip(ring, basis, gridSize = 16, thickness = thickness, distanceTolerance = 0.25)
            val uses = edgeUse(solid)
            assertTrue(uses.values.all { it == 2 }, "thickness $thickness: edges used ${uses.values.groupingBy { it }.eachCount()}")
            // the winding of every triangle agrees with its normals
            for (t in 0 until solid.size / 3) {
                val (a, b, c) = (0..2).map { solid[3 * t + it] }
                val n = (b.position - a.position).cross(c.position - a.position)
                if (n.length < 1E-12) continue
                assertTrue(n.dot(a.normal + b.normal + c.normal) > 0.0, "thickness $thickness, triangle $t")
            }
        }
    }

    @Test
    fun noTriangleSpansMoreThanACell() {
        val clip = PatchClip(ring, basis, gridSize = 12, distanceTolerance = 0.25)
        for (t in 0 until clip.triangles.size / 3) {
            val corners = (0..2).map { clip.triangles[3 * t + it] }
            val cx = (corners.sumOf { it.x } / 3 * 12).let { floor(it) }
            val cy = (corners.sumOf { it.y } / 3 * 12).let { floor(it) }
            assertTrue(corners.all { it.x * 12 >= cx - 1E-9 && it.x * 12 <= cx + 1 + 1E-9 && it.y * 12 >= cy - 1E-9 && it.y * 12 <= cy + 1 + 1E-9 })
        }
    }

    @Test
    fun theAreaOnAFlatPatchIsThatOfTheShape() {
        // a flat patch that maps the basis rectangle to itself, in the plane z = 0
        val flat = BezierPatch3D(List(4) { j -> List(4) { i -> Vector3(i * 100.0 / 3.0, j * 60.0 / 3.0, 0.0) } })
        val surface = flat.clip(ring, basis, gridSize = 8, distanceTolerance = 0.05)
        val area = (0 until surface.size / 3).sumOf { t ->
            val (a, b, c) = (0..2).map { surface[3 * t + it].position }
            (b - a).cross(c - a).length / 2.0
        }
        assertEquals(PI * (25.0 * 25.0 - 10.0 * 10.0), area, 1.0)
        // it faces along the normal of the patch
        assertTrue(surface.all { abs(it.normal.z - 1.0) < 1E-9 || abs(it.normal.z + 1.0) < 1E-9 })
        val n = flat.normal(0.5, 0.5)
        assertTrue(surface.all { it.normal.distanceTo(n) < 1E-9 })
    }

    @Test
    fun theRimFollowsTheEnvelopedOutline() {
        val clip = PatchClip(ring, basis, gridSize = 16, distanceTolerance = 0.1)
        val outline = ring.contours.flatMap { c -> c.toPath3D().envelope(basis, curved, errorTolerance = 1E-4).let { p -> (0..2000).map { p.position(it / 2000.0) } } }
        for ((a, _) in clip.boundary) {
            val p = curved.position(a.x, a.y)
            assertTrue(outline.minOf { it.distanceTo(p) } < 0.05)
        }
    }
}
