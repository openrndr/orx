import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.meshgenerators.TriangleMeshBuilder
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.extra.textoncontour.extrudeText
import org.openrndr.extra.textoncontour.layoutTextOnPath3D
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.typesetText
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExtrudedText {
    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    private val face by lazy { loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0) }

    /** how often every edge of the triangles of [builder] is used, by the positions of its ends */
    private fun edgeUse(builder: TriangleMeshBuilder): Map<Pair<List<Long>, List<Long>>, Int> {
        fun key(p: Vector3) = listOf(round(p.x * 1E6).toLong(), round(p.y * 1E6).toLong(), round(p.z * 1E6).toLong())
        val count = mutableMapOf<Pair<List<Long>, List<Long>>, Int>()
        val d = builder.data
        for (t in 0 until d.size / 3) {
            val c = (0..2).map { key(d[3 * t + it].position) }
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

    /** every triangle of [builder] is wound so that its normals point where it faces */
    private fun windingAgrees(builder: TriangleMeshBuilder): Boolean {
        val d = builder.data
        return (0 until d.size / 3).all { t ->
            val (a, b, c) = (0..2).map { d[3 * t + it] }
            val n = (b.position - a.position).cross(c.position - a.position)
            n.length < 1E-12 || n.dot(a.normal + b.normal + c.normal) > 0.0
        }
    }

    @Test
    fun typesetTextBecomesClosedSolidLetters() {
        val lines = typesetText(listOf(TextSpan("oe8B", null)), listOf(Rectangle(0.0, 0.0, 400.0, 100.0)), TextStyle.Defaults.copy(face = face))
        val builder = TriangleMeshBuilder().apply { extrudeText(lines, depth = 4.0) }
        assertTrue(builder.data.isNotEmpty())
        val uses = edgeUse(builder)
        assertTrue(uses.values.all { it == 2 }, "edges used ${uses.values.groupingBy { it }.eachCount()}")
        assertTrue(windingAgrees(builder))
        // centered on the plane of the text, as deep as asked
        assertEquals(-2.0, builder.data.minOf { it.position.z }, 1E-9)
        assertEquals(2.0, builder.data.maxOf { it.position.z }, 1E-9)
        // not centered it goes from the plane along z
        val forward = TriangleMeshBuilder().apply { extrudeText(lines, depth = 4.0, center = false) }
        assertEquals(0.0, forward.data.minOf { it.position.z }, 1E-9)
        assertEquals(4.0, forward.data.maxOf { it.position.z }, 1E-9)
    }

    @Test
    fun textAlongAHelixIsSolidAtItsDepthInSceneUnits() {
        val helix = List(33) { i -> Vector3(cos(-i * PI / 8) * 6.0, i * 0.25, sin(-i * PI / 8) * 6.0) }.catmullRom(0.5, false).toPath3D()
        // text a unit high, placed by the path
        val style = TextStyle.Defaults.copy(face = face, sizeInEm = 1.0 / 16.0)
        val layout = layoutTextOnPath3D(listOf(TextSpan("solid", null)), helix, style = style, startDistance = 2.0)
        val builder = TriangleMeshBuilder().apply { extrudeText(layout, depth = 0.3) }
        val uses = edgeUse(builder)
        assertTrue(uses.values.all { it == 2 }, "edges used ${uses.values.groupingBy { it }.eachCount()}")
        assertTrue(windingAgrees(builder))
        // the caps are 0.3 apart in space: every vertex of a cap has one of the other cap 0.3 away along its normal
        val caps = builder.data.filter { abs(it.normal.dot(it.normal)) > 0.0 }
        val first = layout.glyphs.first()
        val z = Vector3(first.transform.c2r0, first.transform.c2r1, first.transform.c2r2).normalized
        val front = builder.data.filter { it.normal.distanceTo(z) < 1E-6 }
        val back = builder.data.filter { it.normal.distanceTo(-z) < 1E-6 }
        assertTrue(front.isNotEmpty() && back.isNotEmpty() && caps.isNotEmpty())
        val v = front.first()
        assertTrue(back.any { it.position.distanceTo(v.position - z * 0.3) < 1E-6 })
    }
}
