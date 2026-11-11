import org.openrndr.extra.meshgenerators.extrudeContourAdaptive
import org.openrndr.extra.meshgenerators.extrudeContourSteps
import org.openrndr.extra.meshgenerators.extrudeShape
import org.openrndr.extra.meshgenerators.extrudeShapeAdaptive
import org.openrndr.extra.meshgenerators.extrudeShapeSteps
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExtrusion {
    private class Vertex(val position: Vector3, val normal: Vector3)

    /** a circle of [radius] about the y axis in the plane y = 0, all of it or the first three quarters */
    private fun ring(radius: Double, closed: Boolean): Path3D {
        val circle = Circle(0.0, 0.0, radius).contour
        val segments = circle.segments.let { if (closed) it else it.take(3) }.map { s ->
            fun lift(p: Vector2) = Vector3(p.x, 0.0, p.y)
            Segment3D(lift(s.start), s.control.map(::lift), lift(s.end))
        }
        return Path3D(segments, closed)
    }

    private fun collect(build: ((Vector3, Vector3, Vector2) -> Unit) -> Unit): List<Vertex> {
        val result = mutableListOf<Vertex>()
        build { p, n, _ -> result.add(Vertex(p, n)) }
        return result
    }

    private fun degrees(a: Vector3, b: Vector3) = acos((a.normalized dot b.normalized).coerceIn(-1.0, 1.0)) * 180 / PI

    /** checks the mesh of [triangles] around the ring of [radius]: normals, winding, and that it is closed */
    private fun check(label: String, triangles: List<Vertex>, radius: Double, maxNormalError: Double) {
        assertTrue(triangles.isNotEmpty(), "$label: no triangles")
        for (v in triangles) assertEquals(1.0, v.normal.length, 1E-9, "$label: normal of length ${v.normal.length}")
        var worst = 0.0
        for (t in 0 until triangles.size / 3) {
            val (a, b, c) = (0..2).map { triangles[3 * t + it] }
            val face = (b.position - a.position) cross (c.position - a.position)
            if (face.length < 1E-12) continue
            assertTrue((face dot (a.normal + b.normal + c.normal)) > 0.0, "$label: triangle $t wound against its normals")
            // side vertices: the normal points away from the nearest point of the circle at the centre
            val centroidNormal = (a.normal + b.normal + c.normal).normalized
            if (abs(centroidNormal.y) > 0.99 || abs(face.normalized.y) > 0.99) continue
            val isCap = triangles[3 * t].let { v ->
                val centre = Vector3(v.position.x, 0.0, v.position.z).let { if (it.length > 0) it.normalized * radius else it }
                abs((v.position - centre).normalized dot face.normalized) < 0.5
            }
            if (isCap) continue
            for (v in listOf(a, b, c)) {
                val centre = Vector3(v.position.x, 0.0, v.position.z).normalized * radius
                worst = maxOf(worst, degrees(v.normal, v.position - centre))
            }
        }
        assertTrue(worst < maxNormalError, "$label: normals up to $worst degrees off")
        // closed: every edge is shared by two triangles, and no triangle is there twice
        fun key(p: Vector3) = "${round(p.x * 1E6).toLong()} ${round(p.y * 1E6).toLong()} ${round(p.z * 1E6).toLong()}"
        val edges = mutableMapOf<Pair<String, String>, Int>()
        val seen = mutableMapOf<List<String>, Int>()
        for (t in 0 until triangles.size / 3) {
            val k = (0..2).map { key(triangles[3 * t + it].position) }
            seen[k.sorted()] = (seen[k.sorted()] ?: 0) + 1
            for (i in 0..2) {
                val x = k[i]
                val y = k[(i + 1) % 3]
                if (x == y) continue
                val e = if (x < y) x to y else y to x
                edges[e] = (edges[e] ?: 0) + 1
            }
        }
        val open = edges.values.count { it != 2 }
        assertEquals(0, open, "$label: $open edges not shared by two triangles")
        assertEquals(0, seen.values.sumOf { it - 1 }, "$label: triangles written twice")
    }

    private val ccw = Circle(0.0, 0.0, 2.0).contour
    private val cw = ccw.reversed

    @Test
    fun everyExtrusionHasOutwardNormalsAndIsClosed() {
        for (closed in listOf(false, true)) {
            val path = ring(20.0, closed)
            for ((name, c) in listOf("ccw" to ccw, "cw" to cw)) {
                val label = "${if (closed) "closed" else "open"} $name"
                check("contourSteps $label", collect { w ->
                    extrudeContourSteps(c, path, 120, Vector3.UNIT_Y, 0.05, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                check("contourAdaptive $label", collect { w ->
                    extrudeContourAdaptive(c, path, Vector3.UNIT_Y, 0.05, 0.05, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                check("shapeSteps $label", collect { w ->
                    extrudeShapeSteps(c.shape, path, 120, Vector3.UNIT_Y, 0.05, startCap = true, endCap = true, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                check("shapeAdaptive $label", collect { w ->
                    extrudeShapeAdaptive(c.shape, path, Vector3.UNIT_Y, 0.05, 0.05, startCap = true, endCap = true, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                check("shape(scale, torsion) $label", collect { w ->
                    extrudeShape(c.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y, 0.05, 0.05, startCap = true, endCap = true, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                check("face normals $label", collect { w ->
                    extrudeShape(c.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y, 0.05, 0.05, startCap = true, endCap = true, useFaceNormals = true, writer = w)
                }, 20.0, 10.0)
            }
        }
    }

    @Test
    fun theExtrusionReachesTheEndOfThePath() {
        val path = ring(20.0, false)
        val triangles = collect { w -> extrudeContourSteps(ccw, path, 40, Vector3.UNIT_Y, writer = w, useFaceNormals = false) }
        val end = path.position(1.0)
        // the end cap is centered on the end of the path
        val far = triangles.filter { abs(it.normal dot path.direction(1.0).normalized) > 0.999 && it.position.distanceTo(end) < 2.5 }
        assertTrue(far.isNotEmpty(), "no end cap at the end of the path")
    }

    @Test
    fun holesFaceIntoTheHole() {
        val path = ring(20.0, false)
        val hole = Circle(0.0, 0.0, 1.0).contour.reversed
        val shape = Shape(listOf(ccw, hole))
        val triangles = collect { w ->
            extrudeShapeSteps(shape, path, 60, Vector3.UNIT_Y, 0.05, startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }
        // closed and wound to agree with the normals; the walls are judged below
        check("ring", triangles, 20.0, 181.0)
        // the inner wall faces its axis
        for (t in 0 until triangles.size / 3) {
            val v = triangles[3 * t]
            val centre = Vector3(v.position.x, 0.0, v.position.z).normalized * 20.0
            val r = v.position.distanceTo(centre)
            if (abs(r - 1.0) < 0.01 && abs((v.position - centre).normalized dot v.normal) > 0.5) {
                assertTrue(((v.position - centre) dot v.normal) < 0.0, "the wall of the hole faces out of it")
            }
        }
    }

    @Test
    fun scaledExtrusionsAreClosedWhereTheScaleChanges() {
        val path = ring(20.0, false)
        val triangles = collect { w ->
            extrudeShape(ccw.shape, path, { t -> 0.5 + t }, { t -> t * 90.0 }, Vector3.UNIT_Y, 0.05, 0.05, startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }
        // the normals of the cone-like side are not radial: only closedness, winding and unit normals are checked
        check("scaled", triangles, 20.0, 181.0)
    }

    @Test
    fun stepsDoNotDependOnUnits() {
        fun count(unit: Double): Int = collect { w ->
            val path = ring(20.0 * unit, false)
            extrudeShape(
                Circle(0.0, 0.0, 2.0 * unit).contour.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y,
                0.05 * unit, 0.05 * unit, startCap = false, endCap = false, useFaceNormals = false, writer = w
            )
        }.size
        assertEquals(count(1.0), count(50.0))
    }
}
