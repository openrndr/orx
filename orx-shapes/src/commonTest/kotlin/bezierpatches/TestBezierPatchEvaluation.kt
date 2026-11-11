package bezierpatches

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.bezierpatches.BezierPatch
import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.distort
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestBezierPatchEvaluation {
    private val random = Random(7)

    private fun b(t: Double, k: Int): Double {
        val s = 1 - t
        return listOf(s * s * s, 3 * s * s * t, 3 * s * t * t, t * t * t)[k]
    }

    private fun patch3D() = BezierPatch3D(
        List(4) { j ->
            List(4) { i ->
                Vector3(i.toDouble(), j.toDouble(), 0.0) +
                        Vector3(random.nextDouble(-0.4, 0.4), random.nextDouble(-0.4, 0.4), random.nextDouble(-1.0, 1.0))
            }
        },
        List(4) { j -> List(4) { i -> ColorRGBa(i / 3.0, j / 3.0, random.nextDouble(), 1.0) } }
    )

    private fun patch2D() = BezierPatch(
        List(4) { j ->
            List(4) { i -> Vector2(i * 100.0, j * 100.0) + Vector2(random.nextDouble(-40.0, 40.0), random.nextDouble(-40.0, 40.0)) }
        },
        List(4) { j -> List(4) { i -> ColorRGBa(i / 3.0, j / 3.0, random.nextDouble(), 1.0) } }
    )

    /** the color of a grid of control colors at [u], [v] */
    private fun colorAt(colors: List<List<ColorRGBa>>, u: Double, v: Double): ColorRGBa {
        var r = 0.0; var g = 0.0; var bl = 0.0
        for (j in 0 until 4) for (i in 0 until 4) {
            val w = b(u, i) * b(v, j)
            r += colors[j][i].r * w; g += colors[j][i].g * w; bl += colors[j][i].b * w
        }
        return ColorRGBa(r, g, bl, 1.0)
    }

    private fun ColorRGBa.distanceTo(o: ColorRGBa) = Vector3(r - o.r, g - o.g, b - o.b).length

    @Test
    fun positionsAndDerivativesOf3DPatches() {
        repeat(10) {
            val p = patch3D()
            repeat(50) {
                val u = random.nextDouble()
                val v = random.nextDouble()
                var expected = Vector3.ZERO
                for (j in 0 until 4) for (i in 0 until 4) expected += p.points[j][i] * (b(u, i) * b(v, j))
                assertTrue(p.position(u, v).distanceTo(expected) < 1E-12)
                val h = 1E-5
                val du = (p.position(u + h, v) - p.position(u - h, v)) / (2 * h)
                val dv = (p.position(u, v + h) - p.position(u, v - h)) / (2 * h)
                val duv = (p.position(u + h, v + h) - p.position(u + h, v - h) - p.position(u - h, v + h) + p.position(u - h, v - h)) / (4 * h * h)
                assertTrue(p.du(u, v).distanceTo(du) < 1E-6 * du.length)
                assertTrue(p.dv(u, v).distanceTo(dv) < 1E-6 * dv.length)
                assertTrue(p.duv(u, v).distanceTo(duv) < 1E-4 * (1 + duv.length), "${p.duv(u, v)} $duv")
                @Suppress("DEPRECATION")
                assertTrue((p.gradient(u, v) * 9.0).distanceTo(p.duv(u, v)) < 1E-9)
                val n = p.normal(u, v)
                assertTrue(n.distanceTo(p.du(u, v).cross(p.dv(u, v)).normalized) < 1E-12)
                val f = p.frame(u, v)
                assertTrue(Vector3(f.c3r0, f.c3r1, f.c3r2).distanceTo(expected) < 1E-12)
                assertTrue(Vector3(f.c2r0, f.c2r1, f.c2r2).distanceTo(n) < 1E-12)
            }
        }
    }

    @Test
    fun normalsHaveUnitLengthOnDegeneratePatches() {
        val point = BezierPatch3D(List(4) { List(4) { Vector3(1.0, 2.0, 3.0) } })
        // a sphere octant, its first row collapsed to the pole
        val octant = BezierPatch3D(List(4) { j ->
            List(4) { i ->
                val a = j / 3.0 * PI / 2
                val c = i / 3.0 * PI / 2
                if (j == 0) Vector3(0.0, 0.0, 1.0) else Vector3(cos(c) * sin(a), sin(c) * sin(a), cos(a))
            }
        })
        for (p in listOf(point, octant)) for (u in listOf(0.0, 0.5, 1.0)) for (v in listOf(0.0, 0.5, 1.0)) {
            assertEquals(1.0, p.normal(u, v).length, 1E-9)
            val f = p.frame(u, v)
            assertEquals(1.0, Vector3(f.c0r0, f.c0r1, f.c0r2).length, 1E-9)
        }
    }

    @Test
    fun subPatchesKeepPositionsAndColors() {
        val p3 = patch3D()
        val p2 = patch2D()
        repeat(50) {
            val u0 = random.nextDouble(0.0, 0.5); val u1 = random.nextDouble(0.5, 1.0)
            val v0 = random.nextDouble(0.0, 0.5); val v1 = random.nextDouble(0.5, 1.0)
            val s3: BezierPatch3D = p3.sub(u0, v0, u1, v1)
            val s2: BezierPatch = p2.sub(u0, v0, u1, v1)
            val a = random.nextDouble(); val c = random.nextDouble()
            val u = u0 + a * (u1 - u0); val v = v0 + c * (v1 - v0)
            assertTrue(s3.position(a, c).distanceTo(p3.position(u, v)) < 1E-12)
            assertTrue(s2.position(a, c).distanceTo(p2.position(u, v)) < 1E-9)
            assertTrue(colorAt(s3.colors, a, c).distanceTo(colorAt(p3.colors, u, v)) < 1E-12)
            assertTrue(colorAt(s2.colors, a, c).distanceTo(colorAt(p2.colors, u, v)) < 1E-12)
        }
    }

    @Test
    fun operationsKeepTheTypeOfThePatch() {
        val p3 = patch3D()
        val m: Matrix44 = buildTransform { translate(1.0, 2.0, 3.0); rotate(Vector3.UNIT_Z, 30.0) }
        val results3: List<BezierPatch3D> = listOf(p3.transposed, p3.transform(m), p3.sub(0.0, 0.0, 0.5, 0.5), p3 * 2.0, p3 / 2.0, p3 + p3, p3 - p3)
        assertEquals(7, results3.size)
        assertTrue(p3.transposed.position(0.2, 0.7).distanceTo(p3.position(0.7, 0.2)) < 1E-12)
        val p2 = patch2D()
        val results2: List<BezierPatch> = listOf(p2.transposed, p2.transform(m), p2.sub(0.0, 0.0, 0.5, 0.5), p2 * 2.0, p2 / 2.0, p2 + p2, p2 - p2)
        assertEquals(7, results2.size)
    }

    @Test
    fun derivativesOf2DPatches() {
        val p = patch2D()
        repeat(50) {
            val u = random.nextDouble()
            val v = random.nextDouble()
            val h = 1E-5
            val du = (p.position(u + h, v) - p.position(u - h, v)) / (2 * h)
            val dv = (p.position(u, v + h) - p.position(u, v - h)) / (2 * h)
            assertTrue(p.du(u, v).distanceTo(du) < 1E-6 * du.length)
            assertTrue(p.dv(u, v).distanceTo(dv) < 1E-6 * dv.length)
            @Suppress("DEPRECATION")
            assertTrue((p.gradient(u, v) * 9.0).distanceTo(p.duv(u, v)) < 1E-6)
        }
    }

    @Test
    fun distortedContoursFollowThePatch() {
        val p = patch2D()
        val reference = Rectangle(0.0, 0.0, 300.0, 300.0)
        for (contour in listOf(Circle(150.0, 150.0, 120.0).contour, Rectangle(30.0, 30.0, 240.0, 240.0).contour)) {
            for (tolerance in listOf(0.5, 0.05)) {
                val distorted = p.distort(contour, reference, tolerance)
                for (segment in contour.segments) for (q in 0..40) {
                    val o = segment.position(q / 40.0)
                    val exact = p.position(o.x / 300.0, o.y / 300.0)
                    val off = distorted.nearest(exact).position.distanceTo(exact)
                    assertTrue(off < tolerance, "$off off the image at tolerance $tolerance")
                }
            }
        }
    }
}
