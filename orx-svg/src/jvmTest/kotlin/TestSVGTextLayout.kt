import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestSVGTextLayout {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">$content</svg>"""
    )

    private fun Composition.texts() = root.findAll { it is TextNode }.map { it as TextNode }
    private fun Composition.group(id: String) = root.findGroups().first { it.id == id }

    @Test
    fun handlesWhiteSpace() {
        val c = parse(
            """
            <text id="normal">  a   b
              c  </text>
            <text id="pre" style="white-space: pre">  a   b
            	c  </text>
            <g xml:space="preserve"><text id="preserve"> a  b </text></g>
            <text id="line" style="white-space: pre-line">a   b  
              c</text>
            """
        )
        val t = c.texts().map { it.text }
        assertEquals("a b c", t[0])
        assertEquals("  a   b\n             c  ", t[1])
        assertEquals(" a  b ", t[2])
        assertEquals("a b\nc", t[3])
        assertEquals(WhiteSpace.PRE, c.group("pre").style.whiteSpace)
    }

    @Test
    fun readsTextLengthAndWrapping() {
        val c = parse(
            """
            <text id="a" textLength="50%" lengthAdjust="spacingAndGlyphs" style="inline-size: 120px; line-height: 1.5">a<tspan id="b" textLength="30">b</tspan></text>
            <text id="c" font-size="10" style="line-height: 150%; inline-size: auto">c</text>
            """
        )
        assertEquals(100.0, (c.group("a").style.textLength.value as Length.Pixels).value)
        assertEquals(LengthAdjust.SPACING_AND_GLYPHS, c.group("a").style.lengthAdjust)
        assertEquals(30.0, (c.group("b").style.textLength.value as Length.Pixels).value)
        assertEquals(LengthAdjust.SPACING, c.group("b").style.lengthAdjust)
        assertEquals(120.0, (c.group("a").style.inlineSize.value as Length.Pixels).value)
        assertEquals(1.5, assertIs<LineHeight.Multiple>(c.group("a").style.lineHeight).value)
        assertEquals(15.0, assertIs<LineHeight.Absolute>(c.group("c").style.lineHeight).value, 1E-9)
        assertEquals(null, c.group("c").style.inlineSize.value)
    }

    @Test
    fun readsFontSettings() {
        val c = parse(
            """
            <text id="a" style="font-variant: small-caps tabular-nums; font-feature-settings: 'liga' 0, 'ss01', 'tnum' off">a</text>
            <text id="b" style="font-variation-settings: 'wght' 550, 'wdth' 80">b</text>
            """
        )
        assertEquals(mapOf("smcp" to 1.0, "tnum" to 0.0, "liga" to 0.0, "ss01" to 1.0), c.group("a").style.fontFeatureSettings.value)
        assertEquals(mapOf("wght" to 550.0, "wdth" to 80.0), c.group("b").style.fontVariationSettings.value)
    }

    @Test
    fun writesTextLayout() {
        val c = parse(
            """
            <text id="a" textLength="50" lengthAdjust="spacingAndGlyphs" style="inline-size: 120px; line-height: 1.5; white-space: pre; font-feature-settings: 'smcp'; font-variation-settings: 'wght' 600">a  b</text>
            """
        )
        val written = writeSVG(c)
        assertTrue("textLength=\"50.0\"" in written)
        assertTrue("lengthAdjust=\"spacingAndGlyphs\"" in written)
        assertTrue("inline-size=\"120.0\"" in written)
        val reread = parseSVG(written)
        val a = reread.root.findGroups().first { it.id == "a" }
        assertEquals(WhiteSpace.PRE, a.style.whiteSpace)
        assertEquals("a  b", reread.root.findAll { it is TextNode }.map { (it as TextNode).text }.single())
        assertEquals(1.5, (a.style.lineHeight as LineHeight.Multiple).value)
        assertEquals(mapOf("smcp" to 1.0), a.style.fontFeatureSettings.value)
        assertEquals(mapOf("wght" to 600.0), a.style.fontVariationSettings.value)
    }
}
