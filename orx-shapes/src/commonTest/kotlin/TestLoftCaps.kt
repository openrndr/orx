import org.openrndr.extra.shapes.loft.Loft
import org.openrndr.extra.shapes.loft.LoftVertex
import org.openrndr.extra.shapes.loft.loft
import org.openrndr.extra.shapes.pose.pose
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Segment2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestLoftCaps {
    /** an open helix, two turns */
    private val helix = List(17) { i -> Vector3(cos(i * PI / 4) * 5, i * 0.6, sin(i * PI / 4) * 5) }
        .catmullRom(0.5, false).toPath3D().rectified().pose(Vector3.UNIT_Y)

    /** a star of 5 points, concave, counterclockwise or clockwise */
    private fun star(clockwise: Boolean): ShapeContour {
        val points = List(10) { i ->
            val a = (if (clockwise) -1 else 1) * i * PI / 5
            val r = if (i % 2 == 0) 1.0 else 0.45
            Vector2(cos(a) * r, sin(a) * r)
        }
        return ShapeContour.fromPoints(points, closed = true)
    }

    /** the edges of [triangles], by the positions of their corners, rounded to find shared ones, and how often each is used */
    private fun edges(triangles: List<LoftVertex>): Map<Pair<String, String>, Int> {
        fun key(p: Vector3) = listOf(p.x, p.y, p.z).joinToString(" ") { kotlin.math.round(it * 1E6).toLong().toString() }
        val count = mutableMapOf<Pair<String, String>, Int>()
        for (t in 0 until triangles.size / 3) {
            val c = (0..2).map { key(triangles[3 * t + it].position) }
            for (i in 0..2) {
                val a = c[i]
                val b = c[(i + 1) % 3]
                if (a == b) continue
                val e = if (a < b) a to b else b to a
                count[e] = (count[e] ?: 0) + 1
            }
        }
        return count
    }

    /** the triangle [t] of [triangles] has its winding and its vertex normals agree */
    private fun agrees(triangles: List<LoftVertex>, t: Int): Boolean {
        val (a, b, c) = (0..2).map { triangles[3 * t + it] }
        val geometric = (b.position - a.position).cross(c.position - a.position)
        if (geometric.length < 1E-12) return true
        return geometric.dot(a.normal + b.normal + c.normal) > 0.0
    }

    @Test
    fun aCappedTubeIsClosed() {
        for (clockwise in listOf(false, true)) {
            val loft = star(clockwise).rectified().loft(helix)
            val triangles = loft.side(64, 40) + loft.cap(false, 40) + loft.cap(true, 40)
            val edges = edges(triangles)
            assertTrue(edges.values.all { it == 2 }, "clockwise $clockwise: edges used ${edges.values.groupingBy { it }.eachCount()}")
            assertTrue((0 until triangles.size / 3).all { agrees(triangles, it) }, "clockwise $clockwise: winding and normals")
        }
    }

    @Test
    fun normalsPointOutOfTheLoft() {
        val loft = Circle(0.0, 0.0, 1.0).contour.rectified().loft(helix)
        // the side away from the path
        val side = loft.side(32, 24)
        val poses = (0..32).map { helix.pose(it / 32.0) }
        for (v in side) {
            val j = (v.texCoord.y * 32).toInt().coerceIn(0, 32)
            val centre = Vector3(poses[j].c3r0, poses[j].c3r1, poses[j].c3r2)
            assertTrue(v.normal.dot(v.position - centre) > 0.0)
        }
        // the caps back along the path at the start and along it at the end
        val start = helix.pose(0.0)
        val end = helix.pose(1.0)
        assertTrue(loft.cap(false, 24).all { it.normal.distanceTo(-Vector3(start.c2r0, start.c2r1, start.c2r2)) < 1E-9 })
        assertTrue(loft.cap(true, 24).all { it.normal.distanceTo(Vector3(end.c2r0, end.c2r1, end.c2r2)) < 1E-9 })
    }

    @Test
    fun ribbonsAndRingsHaveNoCaps() {
        val ribbon = ShapeContour.fromSegments(listOf(Segment2D(Vector2(-1.0, 0.0), Vector2(1.0, 0.0))), closed = false).rectified().loft(helix)
        assertTrue(ribbon.cap(false, 8).isEmpty() && ribbon.cap(true, 8).isEmpty())
        assertEquals(8 * 4 * 6, ribbon.side(8, 4).size)
        val ring = List(6) { i -> Vector3(cos(i * PI / 3) * 5, 0.0, sin(i * PI / 3) * 5) }.catmullRom(0.5, true).toPath3D().rectified().pose(Vector3.UNIT_Y)
        val tube = Circle(0.0, 0.0, 1.0).contour.rectified().loft(ring)
        assertTrue(tube.cap(false, 8).isEmpty())
        // a loft made directly has caps unless it says otherwise
        val direct = Loft(crossSection = { u, _ -> Vector2(cos(u * 2 * PI), sin(u * 2 * PI)) }, pose = { v -> helix.pose(v) })
        assertTrue(direct.cap(true, 16).isNotEmpty())
    }
}
