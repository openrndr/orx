import org.openrndr.draw.font.MathConstant
import org.openrndr.draw.font.MathTable
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.math.*
import org.openrndr.fontdriver.freetype.FaceFreetype
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** math layout with STIX Two Math, which macOS has; the tests pass without checking anything where it is missing */
class TestMathLayout {
    private val fontFile = File("/System/Library/Fonts/Supplemental/STIXTwoMath.otf")

    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
    }

    private class Math(val face: FaceFreetype, val table: MathTable) {
        fun layout(mathml: String, display: Boolean = false) = layoutMath(parseMathML(mathml), face, table, display = display)
        fun glyph(codePoint: Int) = TextShapingDriver.instance.shape(face, String(Character.toChars(codePoint))).single().glyphIndex
    }

    private fun math(test: Math.() -> Unit) {
        if (!fontFile.exists()) return
        val face = loadFace(fontFile.absolutePath).withSize(48.0) as FaceFreetype
        Math(face, TextShapingDriver.instance.mathTable(face) ?: error("no MATH table")).test()
    }

    @Test
    fun `MathML is read into nodes, with entities and the attributes that layout uses`() {
        val node = parseMathML(
            """<?xml version="1.0"?><!-- a comment --><math display="block" xmlns="http://www.w3.org/1998/Math/MathML">
                <mfrac linethickness="0"><mi mathvariant="normal">x</mi><mo stretchy="false" lspace="thinmathspace">&sum;&#x2062;&lt;</mo></mfrac>
            </math>"""
        )
        assertEquals(
            MathNode.Formula(
                MathNode.Fraction(
                    MathNode.Identifier("x", "normal"),
                    MathNode.Operator("∑⁢<", stretchy = false, leftSpaceInEm = 3 / 18.0),
                    lineThicknessFactor = 0.0
                ), display = true
            ), node
        )
        assertEquals(MathNode.Scripts(MathNode.Identifier("x"), null, MathNode.Number("2")), parseMathML("<msup><mi>x</mi><mn>2</mn></msup>"))
    }

    @Test
    fun `the MATH table is read in pixels at the size of the face`() = math {
        // the shaping driver gives no table for a face without one
        assertEquals(null, TextShapingDriver.instance.mathTable(loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(48.0)))
        // STIX Two Math: an axis height of 258 units of 1000, at 48 pixels
        assertEquals(258 * 48 / 1000.0, table.constant(MathConstant.AXIS_HEIGHT), 0.1)
        assertEquals(70.0, table.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN), 15.0)
    }

    @Test
    fun `single letter identifiers are in mathematical italic, operators are spaced`() = math {
        val x = layout("<mi>x</mi>")
        assertEquals(glyph(0x1D465), x.glyphs.single().glyphIndex)
        assertEquals(glyph('x'.code), layout("<mi mathvariant=\"normal\">x</mi>").glyphs.single().glyphIndex)
        val a = layout("<mi>a</mi>").width
        val b = layout("<mi>b</mi>").width
        val plus = layout("<mo lspace=\"0\" rspace=\"0\">+</mo>").width
        val sum = layout("<mrow><mi>a</mi><mo>+</mo><mi>b</mi></mrow>")
        // a medium space on both sides of a binary operator
        assertEquals(a + b + plus + 2 * 4 / 18.0 * 48.0, sum.width, 0.5)
        // a hyphen is drawn as a minus
        assertEquals(glyph(0x2212), layout("<mo>-</mo>").glyphs.single().glyphIndex)
    }

    @Test
    fun `fractions put the numerator above and the denominator below a rule on the axis`() = math {
        val axis = table.constant(MathConstant.AXIS_HEIGHT)
        for (display in listOf(false, true)) {
            val f = layout("<mfrac><mi>a</mi><mi>b</mi></mfrac>", display)
            val rule = f.rules.single()
            // y down: the rule is centered on the axis above the baseline
            assertEquals(-axis, rule.center.y, 1E-6)
            val (num, den) = f.glyphs
            assertTrue(num.position.y < rule.y && den.position.y > rule.y + rule.height, "$display: ${num.position} ${den.position} $rule")
            // in display style the parts are as large as the formula, inline a step smaller
            assertEquals(if (display) 1.0 else table.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN) / 100.0, num.style.sizeInEm!!, 1E-9)
        }
        assertTrue(layout("<mfrac linethickness=\"0\"><mi>a</mi><mi>b</mi></mfrac>").rules.isEmpty())
    }

    @Test
    fun `superscripts are raised and subscripts lowered, smaller, after the base`() = math {
        val sup = layout("<msup><mi>x</mi><mn>2</mn></msup>")
        val (base, two) = sup.glyphs
        assertTrue(two.position.y < base.position.y && two.position.x > base.position.x)
        assertEquals(table.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN) / 100.0, two.style.sizeInEm!!, 1E-9)
        val both = layout("<msubsup><mi>x</mi><mi>i</mi><mn>2</mn></msubsup>")
        val (_, i, two2) = both.glyphs
        assertTrue(i.position.y > 0.0 && two2.position.y < 0.0)
        // the scripts start at the same place, the superscript after the italic correction
        assertTrue(two2.position.x >= i.position.x)
    }

    @Test
    fun `fences stretch to what they enclose, with variants and then assemblies`() = math {
        val paren = glyph('('.code)
        val plain = layout("<mrow><mo>(</mo><mi>x</mi><mo>)</mo></mrow>")
        assertEquals(paren, plain.glyphs.first().glyphIndex, "a parenthesis around a letter is not enlarged")
        val fraction = "<mfrac><mi>a</mi><mi>b</mi></mfrac>"
        val tall = layout("<mrow><mo>(</mo>$fraction<mo>)</mo></mrow>", display = true)
        val open = tall.glyphs.first()
        assertNotEquals(paren, open.glyphIndex, "a larger variant")
        // covers the fraction
        val ink = face.glyphForIndex(open.glyphIndex).bounds().movedBy(open.position)
        val inner = layout(fraction, display = true)
        assertTrue(ink.y <= -inner.ascent + 2.0 && ink.y + ink.height >= inner.descent - 2.0, "$ink, ${inner.ascent} ${inner.descent}")
        // nested fractions are higher than the largest variant: the parenthesis is assembled from parts
        var nested = fraction
        repeat(5) { nested = "<mfrac>$nested$nested</mfrac>" }
        val assembled = layout("<mrow><mo>(</mo>$nested<mo>)</mo></mrow>", display = true)
        val content = layout(nested, display = true)
        val parts = assembled.glyphs.filter { it.position.x < 1.0 }
        assertTrue(parts.size >= 3, "${parts.size} parts")
        val top = parts.minOf { face.glyphForIndex(it.glyphIndex).bounds().movedBy(it.position).y }
        val bottom = parts.maxOf { face.glyphForIndex(it.glyphIndex).bounds().movedBy(it.position).let { b -> b.y + b.height } }
        assertTrue(top <= -content.ascent + 2.0 && bottom >= content.descent - 2.0, "$top..$bottom for ${content.ascent} ${content.descent}")
    }

    @Test
    fun `radicals cover their radicand with a rule above it, roots have their index before the sign`() = math {
        val sqrt = layout("<msqrt><mi>x</mi></msqrt>")
        val rule = sqrt.rules.single()
        val x = sqrt.glyphs.last()
        val xInk = face.glyphForIndex(x.glyphIndex).bounds().movedBy(x.position)
        assertTrue(rule.y + rule.height <= xInk.y, "the rule is above the radicand: $rule $xInk")
        assertTrue(rule.x <= xInk.x && rule.x + rule.width >= xInk.x + xInk.width - 1E-6)
        val root = layout("<mroot><mi>x</mi><mn>3</mn></mroot>")
        val three = root.glyphs.first()
        assertTrue(three.style.sizeInEm!! < 0.7 && three.position.y < 0.0)
        assertTrue(root.width > sqrt.width)
    }

    @Test
    fun `large operators are enlarged with limits in display style, with scripts inline`() = math {
        val sum = "<munderover><mo>&sum;</mo><mrow><mi>i</mi><mo>=</mo><mn>1</mn></mrow><mi>n</mi></munderover>"
        val display = layout(sum, display = true)
        val inline = layout(sum)
        val sigma = glyph(0x2211)
        val bigSigma = display.glyphs.first()
        assertNotEquals(sigma, bigSigma.glyphIndex, "a display variant")
        val italicN = glyph(0x1D45B)
        // n is above the sum in display style, after it inline
        val n = display.glyphs.single { it.glyphIndex == italicN }
        assertTrue(n.position.y < bigSigma.position.y - face.glyphForIndex(bigSigma.glyphIndex).bounds().height / 2.0)
        val inlineSigma = inline.glyphs.first()
        assertEquals(sigma, inlineSigma.glyphIndex)
        val inlineN = inline.glyphs.single { it.glyphIndex == italicN || it.style.sizeInEm!! < 1.0 && it.position.y < 0.0 }
        assertTrue(inlineN.position.x > inlineSigma.position.x + face.glyphForIndex(sigma).advanceWidth() * 0.9)
        assertTrue(display.ascent > inline.ascent)
    }
}
