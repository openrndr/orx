import org.openrndr.extra.shapes.hobbycurve.HobbyTension
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestHobbyCurve {

    @Test
    fun testSymmetric() {
        val rectangle = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val h = rectangle.hobbyCurve()
        assertTrue(h.closed)
        assertEquals(4, h.segments.size)
        assertEquals(-1.0, h.direction(0.25).dot(h.direction(0.75)), 1e-6)
        assertEquals(-1.0, h.direction(0.125).dot(h.direction(0.625)), 1e-6)
        assertEquals(-1.0, h.direction(0.375).dot(h.direction(0.875)), 1e-6)
    }

    private val zigzag = listOf(
        Vector2(0.0, 0.0), Vector2(100.0, 80.0), Vector2(200.0, -20.0), Vector2(300.0, 90.0), Vector2(380.0, 10.0)
    )

    private fun assertSameCurve(a: ShapeContour, b: ShapeContour) {
        assertEquals(a.segments.size, b.segments.size)
        for ((sa, sb) in a.segments.zip(b.segments)) {
            for ((pa, pb) in (listOf(sa.start, sa.end) + sa.control).zip(listOf(sb.start, sb.end) + sb.control)) {
                assertTrue(pa.distanceTo(pb) < 1E-9, "$pa vs $pb")
            }
        }
    }

    @Test
    fun unitTensionIsHobby() {
        for (closed in listOf(false, true)) for (curl in listOf(0.0, 1.0)) {
            assertSameCurve(hobbyCurve(zigzag, closed, curl), hobbyCurve(zigzag, closed, curl, tension = 1.0))
        }
    }

    @Test
    fun regularPolygon() {
        // by symmetry every segment leaves and arrives at half the turning angle, θ = φ = π/n, with handles
        // ρ(θ, θ) / (3τ) times the chord
        val n = 6
        val points = List(n) { Vector2(cos(it * 2 * PI / n), sin(it * 2 * PI / n)) * 100.0 }
        val theta = PI / n
        val rho = (4 + sqrt(8.0) * (sin(theta) - sin(theta) / 16) * (sin(theta) - sin(theta) / 16) * 0.0) /
                (2 + (sqrt(5.0) - 1) * cos(theta) + (3 - sqrt(5.0)) * cos(theta))
        for (tension in listOf(0.75, 1.0, 2.0, 4.0)) {
            val curve = hobbyCurve(points, closed = true, tension = tension)
            for (segment in curve.segments) {
                val chord = segment.end - segment.start
                val handle = segment.control[0] - segment.start
                assertEquals(rho / (3 * tension) * chord.length, handle.length, 1E-9)
                assertEquals(cos(theta), handle.normalized.dot(chord.normalized), 1E-9)
            }
        }
    }

    /** The largest distance from [curve] to the polyline through [points]. */
    private fun deviation(curve: ShapeContour, points: List<Vector2>): Double {
        val polyline = ShapeContour.fromPoints(points, false)
        return (0..400).maxOf { polyline.nearest(curve.position(it / 400.0)).position.distanceTo(curve.position(it / 400.0)) }
    }

    @Test
    fun higherTensionIsTighter() {
        val deviations = listOf(0.75, 1.0, 1.5, 3.0, 10.0).map { deviation(hobbyCurve(zigzag, tension = it), zigzag) }
        for ((looser, tighter) in deviations.zipWithNext()) assertTrue(tighter < looser, "$deviations")
        assertTrue(deviations.last() < 0.15 * deviations[1], "$deviations")
    }

    /** Whether [p] lies in the closed triangle [a], [b], [c]. */
    private fun inTriangle(p: Vector2, a: Vector2, b: Vector2, c: Vector2): Boolean {
        fun cross(u: Vector2, v: Vector2) = u.x * v.y - u.y * v.x
        val d1 = cross(b - a, p - a)
        val d2 = cross(c - b, p - b)
        val d3 = cross(a - c, p - c)
        val eps = 1E-9 * (b - a).length * (c - a).length
        return (d1 >= -eps && d2 >= -eps && d3 >= -eps) || (d1 <= eps && d2 <= eps && d3 <= eps)
    }

    @Test
    fun atLeast() {
        // with tension atleast every segment without an inflection stays inside the triangle of its chord and tangents
        val random = Random(1)
        var checked = 0
        repeat(30) {
            val points = List(5) { Vector2(random.nextDouble(0.0, 300.0), random.nextDouble(0.0, 300.0)) }
            for (closed in listOf(false, true)) {
                val curve = hobbyCurve(points, closed, tension = 0.75, atLeast = true)
                for (segment in curve.segments) {
                    val p0 = segment.start
                    val p1 = segment.end
                    val d0 = segment.control[0] - p0
                    val d1 = p1 - segment.control[1]
                    val chord = p1 - p0
                    fun cross(u: Vector2, v: Vector2) = u.x * v.y - u.y * v.x
                    // the triangle exists where both tangents lie on the same side of the chord
                    if (cross(chord, d0) * cross(chord, d1) >= 0.0 || cross(chord, d0) == 0.0) continue
                    val s = cross(d0, d1)
                    if (s == 0.0) continue
                    // apex of the triangle: p0 + d0·u = p1 - d1·v; it lies behind the chord where the tangents turn by
                    // more than 180 degrees together, then there is no triangle to stay in
                    val u = cross(chord, d1) / cross(d0, d1)
                    val v = cross(d0, chord) / cross(d0, d1)
                    if (u <= 0.0 || v <= 0.0) continue
                    val apex = p0 + d0 * u
                    assertTrue(inTriangle(segment.control[0], p0, apex, p1), "start handle leaves the triangle")
                    assertTrue(inTriangle(segment.control[1], p0, apex, p1), "end handle leaves the triangle")
                    checked++
                }
            }
        }
        assertTrue(checked > 30, "only $checked segments with a triangle")
    }

    @Test
    fun perSegmentTensions() {
        // a high tension on one segment tightens that segment, the others change less
        val uniform = hobbyCurve(zigzag, tension = 1.0)
        val tight = hobbyCurve(zigzag, segmentTensions = List(4) { if (it == 1) HobbyTension(4.0, 4.0) else HobbyTension() })
        val lengthsUniform = uniform.segments.map(Segment2D::length)
        val lengthsTight = tight.segments.map(Segment2D::length)
        val chord = zigzag[2].distanceTo(zigzag[1])
        assertTrue(lengthsTight[1] < lengthsUniform[1])
        assertTrue(lengthsTight[1] - chord < 0.25 * (lengthsUniform[1] - chord))
        assertFailsWith<IllegalArgumentException> { HobbyTension(0.5, 1.0) }
        assertFailsWith<IllegalArgumentException> { hobbyCurve(zigzag, segmentTensions = List(3) { HobbyTension() }) }
    }

    @Test
    fun symmetry() {
        // a mirror symmetric open curve stays symmetric for any tension and curl
        val points = listOf(Vector2(-200.0, 0.0), Vector2(-100.0, 90.0), Vector2(0.0, 0.0), Vector2(100.0, 90.0), Vector2(200.0, 0.0))
        for (tension in listOf(0.8, 1.7)) for (curl in listOf(0.0, 1.0, 3.0)) {
            val curve = hobbyCurve(points, curl = curl, tension = tension)
            for (k in 0..20) {
                val t = k / 40.0
                val a = curve.position(t)
                val b = curve.position(1.0 - t)
                assertEquals(a.x, -b.x, 1E-6)
                assertEquals(a.y, b.y, 1E-6)
            }
        }
    }

    @Test
    fun threeDimensions() {
        // planar 3D points give the same curve as in 2D
        for (closed in listOf(false, true)) {
            val curve2 = hobbyCurve(zigzag, closed, curl = 0.5, tension = 1.6)
            val curve3 = hobbyCurve(zigzag.map { Vector3(it.x, it.y, 0.0) }, closed, curl = 0.5, tension = 1.6)
            assertEquals(curve2.segments.size, curve3.segments.size)
            for ((s2, s3) in curve2.segments.zip(curve3.segments)) {
                for ((p2, p3) in s2.control.zip(s3.control)) {
                    assertTrue(Vector3(p2.x, p2.y, 0.0).distanceTo(p3) < 1E-9)
                }
            }
        }
    }

    @Test
    fun twoPoints() {
        val curve = hobbyCurve(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0)), curl = 1.0, tension = 1.0)
        assertEquals(1, curve.segments.size)
        assertTrue(curve.segments[0].position(0.5).distanceTo(Vector2(50.0, 0.0)) < 1E-9)
    }
}
