import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGProperties {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">$content</svg>"""
    )

    private fun Composition.texts() = root.findAll { it is TextNode }.map { it as TextNode }

    @Test
    fun placesCharacters() {
        val c = parse(
            """
            <text x="10 20 30" y="50" dy="0 5" rotate="10 20">abcdef<tspan dx="7" rotate="-5">gh</tspan></text>
            """
        )
        val runs = c.texts().map { listOf(it.text, it.x, it.y, it.dx, it.dy, it.rotate) }
        assertEquals(
            listOf(
                listOf("a", 10.0, 50.0, 0.0, 0.0, 10.0),
                listOf("b", 20.0, null, 0.0, 5.0, 20.0),
                // the last rotation applies to the characters after it
                listOf("cdef", 30.0, null, 0.0, 0.0, 20.0),
                // characters that are not positioned and turned alike stay together
                listOf("gh", null, null, 7.0, 0.0, -5.0)
            ).map { it.toString() },
            runs.map { it.toString() }
        )
    }

    @Test
    fun keepsRunsWithoutPositions() {
        val c = parse("""<text x="10" y="20">hello <tspan>world</tspan></text>""")
        assertEquals(listOf("hello ", "world"), c.texts().map { it.text })
        assertEquals(10.0, c.texts().first().x)
        assertNull(c.texts()[1].x)
    }

    @Test
    fun parsesTextProperties() {
        val c = parse(
            """
            <text id="t" font-size="10" letter-spacing="0.5em" word-spacing="3" dominant-baseline="central"
                  text-decoration="underline line-through">a<tspan id="s" baseline-shift="super">b</tspan><tspan id="p" baseline-shift="20%">c</tspan></text>
            """
        )
        val style = c.texts().first().effectiveStyle
        assertEquals(5.0, (style.letterSpacing as Length.Pixels).value, 1E-9)
        assertEquals(3.0, (style.wordSpacing as Length.Pixels).value, 1E-9)
        assertEquals(DominantBaseline.CENTRAL, style.dominantBaseline)
        assertTrue(style.textDecoration.underline && style.textDecoration.lineThrough && !style.textDecoration.overline)
        val groups = c.root.findGroups()
        assertIs<BaselineShift.Super>(groups.first { it.id == "s" }.style.baselineShift)
        assertEquals(20.0, ((groups.first { it.id == "p" }.style.baselineShift as BaselineShift.Value).value as Length.Percent).value)
    }

    @Test
    fun parsesShapeProperties() {
        val c = parse(
            """
            <a href="https://openrndr.org"><path id="p" d="M 0 0 L 100 0" pathLength="10" shape-rendering="crispEdges">
                <title>a path</title><desc>with a length</desc>
            </path></a>
            """
        )
        val path = c.findShapes().single { it.id == "p" }
        assertEquals(10.0, path.style.pathLength.value)
        assertEquals(ShapeRendering.CRISP_EDGES, path.effectiveStyle.shapeRendering)
        assertEquals("a path", path.userData["title"])
        assertEquals("with a length", path.userData["desc"])

        val written = writeSVG(c)
        assertTrue("pathLength=\"10.0\"" in written)
        assertTrue("shape-rendering=\"crispEdges\"" in written)
        assertTrue("<title>a path</title>" in written)
        val reread = parseSVG(written).findShapes().single { it.id == "p" }
        assertEquals(10.0, reread.style.pathLength.value)
        assertEquals("with a length", reread.userData["desc"])
    }

    @Test
    fun writesTextProperties() {
        val c = parse("""<text x="10" y="20" letter-spacing="2" rotate="15" dominant-baseline="middle">ab</text>""")
        val written = writeSVG(c)
        val reread = parseSVG(written).root.findAll { it is TextNode }.map { it as TextNode }.single()
        assertEquals(15.0, reread.rotate)
        assertEquals(2.0, (reread.effectiveStyle.letterSpacing as Length.Pixels).value, 1E-9)
        assertEquals(DominantBaseline.MIDDLE, reread.effectiveStyle.dominantBaseline)
    }
}
