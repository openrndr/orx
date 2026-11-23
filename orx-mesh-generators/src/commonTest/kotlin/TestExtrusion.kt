import org.openrndr.extra.meshgenerators.extrudeContourAdaptive
import org.openrndr.extra.meshgenerators.extrudeContourSteps
import org.openrndr.extra.meshgenerators.extrudeContourStepsMorphed
import org.openrndr.extra.meshgenerators.extrudeContourStepsScaled
import org.openrndr.extra.meshgenerators.extrudeShape
import org.openrndr.extra.meshgenerators.extrudeShapeAdaptive
import org.openrndr.extra.meshgenerators.extrudeShapeSteps
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
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
        // rounded to Int, not Long, which is slow on JS: the meshes stay well within 2000 of the origin
        fun key(p: Vector3) = "${round(p.x * 1E6).toInt()} ${round(p.y * 1E6).toInt()} ${round(p.z * 1E6).toInt()}"
        val edges = mutableMapOf<Pair<String, String>, Int>()
        val seen = mutableMapOf<List<String>, Int>()
        for (t in 0 until triangles.size / 3) {
            val k = (0..2).map { key(triangles[3 * t + it].position) }
            val sorted = k.sorted()
            seen[sorted] = (seen[sorted] ?: 0) + 1
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

    // one test per path and orientation, so that each stays well within the 2 second timeout of the JS tests
    @Test
    fun openPathCcwExtrusionsHaveOutwardNormalsAndAreClosed() = checkExtrusions(closed = false, "ccw", ccw)

    @Test
    fun openPathCwExtrusionsHaveOutwardNormalsAndAreClosed() = checkExtrusions(closed = false, "cw", cw)

    @Test
    fun closedPathCcwExtrusionsHaveOutwardNormalsAndAreClosed() = checkExtrusions(closed = true, "ccw", ccw)

    @Test
    fun closedPathCwExtrusionsHaveOutwardNormalsAndAreClosed() = checkExtrusions(closed = true, "cw", cw)

    private fun checkExtrusions(closed: Boolean, name: String, c: ShapeContour) {
        val path = ring(20.0, closed)
        val label = "${if (closed) "closed" else "open"} $name"
        check("contourSteps $label", collect { w ->
            extrudeContourSteps(c, path, 60, Vector3.UNIT_Y, 0.1, useFaceNormals = false, writer = w)
        }, 20.0, 1.0)
        check("contourAdaptive $label", collect { w ->
            extrudeContourAdaptive(c, path, Vector3.UNIT_Y, 0.1, 0.1, useFaceNormals = false, writer = w)
        }, 20.0, 1.0)
        check("shapeSteps $label", collect { w ->
            extrudeShapeSteps(c.shape, path, 60, Vector3.UNIT_Y, 0.1, startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }, 20.0, 1.0)
        check("shapeAdaptive $label", collect { w ->
            extrudeShapeAdaptive(c.shape, path, Vector3.UNIT_Y, 0.1, 0.1, startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }, 20.0, 1.0)
        check("shape(scale, torsion) $label", collect { w ->
            extrudeShape(c.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y, 0.1, 0.1, startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }, 20.0, 1.0)
        check("face normals $label", collect { w ->
            extrudeShape(c.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y, 0.1, 0.1, startCap = true, endCap = true, useFaceNormals = true, writer = w)
        }, 20.0, 10.0)
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

    /** the largest angle between the vertex normals of [triangles] and the normals of their faces */
    private fun offFaces(triangles: List<Vertex>): Double = (0 until triangles.size / 3).maxOf { t ->
        val (a, b, c) = (0..2).map { triangles[3 * t + it] }
        val face = (b.position - a.position) cross (c.position - a.position)
        if (face.length < 1E-12) 0.0 else listOf(a, b, c).maxOf { degrees(it.normal, face) }
    }

    @Test
    fun scaledExtrusionsHaveOutwardNormalsAndAreClosed() {
        for (closed in listOf(false, true)) {
            val path = ring(20.0, closed)
            for ((name, c) in listOf("ccw" to ccw, "cw" to cw)) {
                val label = "${if (closed) "closed" else "open"} $name"
                check("scaled 1 $label", collect { w ->
                    extrudeContourStepsScaled(c, path, 120, Vector3.UNIT_Y, 0.05, useFaceNormals = false, writer = w)
                }, 20.0, 1.0)
                // a scale that changes along the path tilts the side: smooth normals follow the faces
                val cone = collect { w ->
                    extrudeContourStepsScaled(c, path, 120, Vector3.UNIT_Y, 0.05, scale = { t -> 0.5 + t }, useFaceNormals = false, writer = w)
                }
                if (!closed) check("scaled 0.5 + t $label", cone, 20.0, 181.0)
                assertTrue(offFaces(cone) < 8.0, "scaled 0.5 + t $label: normals up to ${offFaces(cone)} degrees off their faces")
            }
        }
    }

    @Test
    fun scaledExtrusionsUseFaceNormalsWhenAsked() {
        val path = ring(20.0, false)
        val face = collect { w -> extrudeContourStepsScaled(ccw, path, 40, Vector3.UNIT_Y, 0.05, useFaceNormals = true, writer = w) }
        assertTrue(offFaces(face) < 1E-2, "face normals up to ${offFaces(face)} degrees off their faces")
        val smooth = collect { w -> extrudeContourStepsScaled(ccw, path, 40, Vector3.UNIT_Y, 0.05, useFaceNormals = false, writer = w) }
        assertTrue(offFaces(smooth) > 1.0, "smooth normals are those of the faces")
    }

    @Test
    fun scaledExtrusionsEndAtTheEndOfThePathWithTheLastScale() {
        val path = ring(20.0, false)
        val triangles = collect { w ->
            extrudeContourStepsScaled(ccw, path, 40, Vector3.UNIT_Y, 0.05, scale = { t -> 1.0 + t }, useFaceNormals = false, writer = w)
        }
        val end = path.position(1.0)
        val direction = path.direction(1.0).normalized
        val cap = triangles.filter { abs(it.normal dot direction) > 0.999 && abs((it.position - end) dot direction) < 1E-6 }
        assertTrue(cap.isNotEmpty(), "no end cap at the end of the path")
        // scaled by 2 there: the rim of the cap is 4 from the end of the path, as near as Bezier circles are round
        assertEquals(4.0, cap.maxOf { it.position.distanceTo(end) }, 1E-2)
    }

    @Test
    fun hairlineEndsHaveNormalsAndNoCaps() {
        val path = ring(20.0, false)
        val triangles = collect { w ->
            extrudeContourStepsScaled(ccw, path, 60, Vector3.UNIT_Y, 0.05, scale = { t -> 0.5 - 0.5 * cos(t * 2 * PI) }, useFaceNormals = false, writer = w)
        }
        // closed at the points of the ends, without caps there
        check("hairline", triangles, 20.0, 181.0)
        val start = path.position(0.0)
        assertTrue(triangles.none { it.position.distanceTo(start) < 1E-9 && abs(it.normal dot path.direction(0.0).normalized) > 0.999 }, "a cap at a point")
    }

    @Test
    fun morphedExtrusionsHaveOutwardNormalsAndAreClosed() {
        for (closed in listOf(false, true)) {
            val path = ring(20.0, closed)
            for (clockwise in listOf(false, true)) {
                val label = "${if (closed) "closed" else "open"} ${if (clockwise) "cw" else "ccw"}"
                // a circle that grows and shrinks again, which comes round on a closed path
                val triangles = collect { w ->
                    extrudeContourStepsMorphed({ t ->
                        Circle(0.0, 0.0, 1.5 + sin(t * PI)).contour.let { if (clockwise) it.reversed else it }
                    }, path, 120, Vector3.UNIT_Y, 0.05, writer = w)
                }
                check("morphed $label", triangles, 20.0, 181.0)
                assertTrue(offFaces(triangles) < 8.0, "morphed $label: normals up to ${offFaces(triangles)} degrees off their faces")
            }
        }
    }

    @Test
    fun morphedCrossSectionsMayHaveDifferentDetail() {
        val path = ring(20.0, false)
        // from a circle, sampled at many points, to a square, at a few
        val triangles = collect { w ->
            extrudeContourStepsMorphed({ t ->
                if (t < 0.5) Circle(0.0, 0.0, 2.0).contour else Rectangle.fromCenter(Vector2.ZERO, 3.0, 3.0).contour
            }, path, 40, Vector3.UNIT_Y, 0.05, writer = w)
        }
        check("circle to square", triangles, 20.0, 181.0)
    }

    @Test
    fun aCrossSectionThatCrossesItselfIsCapped() {
        // a figure 8, its two lobes wound opposite ways, along a quarter of the ring
        val figure8 = ShapeContour.fromPoints(List(64) { k ->
            val a = 2 * PI * (k + 0.5) / 64
            Vector2(sin(a) * 2.0, sin(a) * cos(a) * 2.0)
        }, true)
        val path = ring(20.0, false)
        val triangles = collect { w ->
            extrudeShape(figure8.shape, path, { 1.0 }, { 0.0 }, Vector3.UNIT_Y, 0.05, 0.05,
                startCap = true, endCap = true, useFaceNormals = false, writer = w)
        }
        // the caps: triangles facing back along the path at its start
        val start = path.position(0.0)
        val back = -path.direction(0.0).normalized
        val cap = (0 until triangles.size / 3).map { t -> (0..2).map { triangles[3 * t + it] } }
            .filter { t -> t.all { abs(it.normal dot back) > 0.999 && abs((it.position - start) dot back) < 1E-6 } }
        val area = cap.sumOf { (a, b, c) -> ((b.position - a.position) cross (c.position - a.position)).length / 2 }
        // two lobes of the figure 8 2 sin a, 2 sin a cos a, each of area 8/3, less the chords of 64 samples
        assertEquals(16.0 / 3.0, area, 0.1)
        for (v in triangles) assertEquals(1.0, v.normal.length, 1E-9)
    }
}
