package bezierpatches

import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.PatchClip
import org.openrndr.extra.shapes.bezierpatches.PatchVertex
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPatchClipCoverage {
    private val basis = Rectangle(0.0, 0.0, 100.0, 60.0)

    /** a patch bent like part of a cylinder around the x axis */
    private val curved = BezierPatch3D(List(4) { j -> List(4) { i ->
        val a = (j / 3.0 - 0.5) * PI / 2
        Vector3(i * 10.0 / 3.0, sin(a) * 6.0, cos(a) * 6.0)
    } })

    /** the area of the triangles of [clip] in the coordinates of the basis */
    private fun area(clip: PatchClip): Double {
        var a = 0.0
        for (t in 0 until clip.triangles.size / 3) {
            val (p, q, r) = (0..2).map { clip.triangles[3 * t + it] }
            a += ((q - p).x * (r - p).y - (q - p).y * (r - p).x) / 2
        }
        return a * basis.width * basis.height
    }

    /** how many triangles of [clip] hold [q], in the coordinates of the basis, strictly inside; off the grid lines */
    private fun cover(clip: PatchClip, q: Vector2): Int = (0 until clip.triangles.size / 3).count { t ->
        val p = (0..2).map { clip.triangles[3 * t + it].let { v -> Vector2(v.x * basis.width, v.y * basis.height) } }
        fun s(a: Vector2, b: Vector2) = (b.x - a.x) * (q.y - a.y) - (q.x - a.x) * (b.y - a.y)
        s(p[0], p[1]) > 1E-9 && s(p[1], p[2]) > 1E-9 && s(p[2], p[0]) > 1E-9
    }

    @Test
    fun overlappingContoursAreCoveredOnce() {
        val overlap = Shape(listOf(Circle(40.0, 30.0, 20.0).contour, Circle(60.0, 30.0, 20.0).contour))
        val clip = PatchClip(overlap, basis, 16, 0.05)
        // the union of two circles 20 apart, radius 20
        val lens = 2 * (400 * kotlin.math.acos(0.5) - 10 * kotlin.math.sqrt(300.0))
        val expected = 2 * PI * 400 - lens
        assertEquals(expected, area(clip), expected * 2E-3)
        // in the lens, where both circles are, once
        for (q in listOf(Vector2(50.3, 30.2), Vector2(50.3, 40.3), Vector2(47.1, 22.2))) {
            assertEquals(1, cover(clip, q), "covered ${cover(clip, q)} times at $q")
        }
    }

    @Test
    fun aSelfCrossingContourIsCoveredByItsFillRule() {
        val figure8 = ShapeContour.fromPoints(List(200) { k ->
            val a = k * 2 * PI / 200
            Vector2(50 + 35 * sin(a), 30 + 20 * sin(a) * cos(a))
        }, true)
        val clip = PatchClip(figure8.shape, basis, 16, 0.05)
        for (q in listOf(Vector2(30.4, 30.3), Vector2(69.6, 30.3))) assertEquals(1, cover(clip, q))
        assertEquals(0, cover(clip, Vector2(50.3, 45.2)))
    }

    @Test
    fun onlyThePartOnThePatchIsKept() {
        val past = Circle(95.0, 30.0, 20.0).shape
        val clip = PatchClip(past, basis, 16, 0.05)
        assertTrue(clip.triangles.all { it.x in -1E-12..1.0 + 1E-12 && it.y in -1E-12..1.0 + 1E-12 })
        // the part of the circle with x up to 100: a circular segment
        val d = 5.0
        val segment = 400 * kotlin.math.acos(-d / 20) + d * kotlin.math.sqrt(400 - d * d)
        assertEquals(segment, area(clip), segment * 2E-3)
        // all of it when asked
        val all = PatchClip(past, basis, 16, 0.05, clipToPatch = false)
        assertEquals(PI * 400, area(all), PI * 400 * 2E-3)
    }

    @Test
    fun aThickClipAcrossTheEdgeIsAClosedSolid() {
        val past = Shape(listOf(Circle(90.0, 30.0, 20.0).contour, Circle(90.0, 30.0, 6.0).contour.reversed))
        val solid = PatchClip(past, basis, 16, 0.1).vertices(curved, 0.5)
        fun key(p: Vector3) = listOf(round(p.x * 1E6).toLong(), round(p.y * 1E6).toLong(), round(p.z * 1E6).toLong())
        val count = mutableMapOf<Pair<List<Long>, List<Long>>, Int>()
        for (t in 0 until solid.size / 3) {
            val c = (0..2).map { key(solid[3 * t + it].position) }
            for (i in 0..2) {
                val a = c[i]
                val b = c[(i + 1) % 3]
                if (a == b) continue
                val e = if (a.toString() < b.toString()) a to b else b to a
                count[e] = (count[e] ?: 0) + 1
            }
        }
        assertTrue(count.values.all { it == 2 }, "edges used ${count.values.groupingBy { it }.eachCount()}")
        for (t in 0 until solid.size / 3) {
            val (a, b, c) = (0..2).map { solid[3 * t + it] }
            val n = (b.position - a.position).cross(c.position - a.position)
            if (n.length < 1E-12) continue
            assertTrue(n.dot(a.normal + b.normal + c.normal) > 0.0)
        }
    }

    @Test
    fun indexedCornersAreTheTriangles() {
        val ring = Shape(listOf(Circle(50.0, 30.0, 25.0).contour, Circle(50.0, 30.0, 10.0).contour.reversed))
        val clip = PatchClip(ring, basis, 32, 0.25)
        assertEquals(clip.triangles.size, clip.indices.size)
        for (k in clip.indices.indices) assertEquals(clip.triangles[k], clip.uvs[clip.indices[k]])
        // no two corners at the same place
        val keys = clip.uvs.map { round(it.x * 1E10) to round(it.y * 1E10) }
        assertEquals(keys.size, keys.toSet().size)
        // vertices at the surface, with the normals of the patch
        val vertices: List<PatchVertex> = clip.vertices(curved)
        assertEquals(clip.triangles.size, vertices.size)
        for ((k, v) in vertices.withIndex()) {
            val uv = clip.triangles[k]
            assertTrue(v.position.distanceTo(curved.position(uv.x, uv.y)) < 1E-12)
            assertTrue(v.normal.distanceTo(curved.normal(uv.x, uv.y)) < 1E-12)
            assertEquals(uv, v.texCoord)
            assertTrue(abs(v.normal.length - 1.0) < 1E-9)
        }
    }
}
