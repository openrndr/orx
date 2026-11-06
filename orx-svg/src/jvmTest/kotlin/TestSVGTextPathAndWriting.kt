import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSVGTextPathAndWriting {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
            <path id="line" d="M 0 50 L 100 50"/>
            <text><textPath href="#line">left</textPath></text>
            <text><textPath href="#line" side="right">right</textPath></text>
            <text><textPath path="M 0 80 L 200 80" href="#line">inline</textPath></text>
            <text id="v" style="writing-mode: vertical-rl; text-orientation: upright">v</text>
            <text id="tb" writing-mode="tb" glyph-orientation-vertical="90">tb</text>
            <text id="rtl" direction="rtl" style="unicode-bidi: bidi-override">rtl<tspan id="inner" unicode-bidi="isolate">a</tspan></text>
        </svg>"""
    )

    private fun run(text: String) = c.root.findAll { it is TextNode && it.text == text }.single() as TextNode

    @Test
    fun placesTextOnPaths() {
        val left = run("left").contour!!
        assertEquals(Vector2(0.0, 50.0), left.segments.first().start)
        // on the right side the text follows the path reversed
        val right = run("right").contour!!
        assertEquals(Vector2(100.0, 50.0), right.segments.first().start)
        // the path attribute is used before the path the textPath element refers to
        val inline = run("inline").contour!!
        assertEquals(Vector2(0.0, 80.0), inline.segments.first().start)
        assertEquals(Vector2(200.0, 80.0), inline.segments.last().end)
    }

    @Test
    fun readsWritingModes() {
        val v = c.root.findGroups().first { it.id == "v" }.style
        assertEquals(WritingMode.VERTICAL_RL, v.writingMode)
        assertEquals(TextOrientation.UPRIGHT, v.textOrientation)
        val tb = c.root.findGroups().first { it.id == "tb" }.style
        assertEquals(WritingMode.VERTICAL_RL, tb.writingMode)
        assertEquals(TextOrientation.SIDEWAYS, tb.textOrientation)
    }

    @Test
    fun readsDirections() {
        val rtl = c.root.findGroups().first { it.id == "rtl" }
        assertEquals(TextDirection.RTL, rtl.style.direction)
        assertEquals(UnicodeBidi.BIDI_OVERRIDE, rtl.style.unicodeBidi)
        val inner = c.root.findGroups().first { it.id == "inner" }
        // the direction is inherited, unicode-bidi is not
        assertEquals(TextDirection.RTL, inner.effectiveStyle.direction)
        assertEquals(UnicodeBidi.ISOLATE, inner.style.unicodeBidi)
        assertEquals(UnicodeBidi.NORMAL, c.root.findGroups().first { it.id == "v" }.effectiveStyle.unicodeBidi)
    }
}
