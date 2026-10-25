import org.openrndr.draw.font.Face
import org.openrndr.draw.font.loadFace
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.Direction
import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.hyphenate
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.extra.textwriter.typesetText
import org.openrndr.math.Vector2
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.shape.Rectangle
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Typesetting needs font and shaping drivers but no window or GL context
class TestTypesetText {
    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    private val text = "The wheels of the bus go round and round, round and round, round and round."

    private fun face(): Face = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(24.0)

    @Test
    fun linesStayInsideTheBox() {
        val box = Rectangle(10.0, 20.0, 200.0, 400.0)
        val lines = typesetText(listOf(TextSpan(text, null)), listOf(box), TextStyle.Defaults.copy(face = face()))
        assertTrue(lines.size > 1)
        for (glyph in lines.flatMap { it.glyphs }) {
            assertTrue(glyph.position.x >= box.x && glyph.position.x < box.x + box.width)
        }
    }

    @Test
    fun firstBaselineIsAtAscentForTopAlignment() {
        val face = face()
        val box = Rectangle(0.0, 50.0, 300.0, 300.0)
        val lines = typesetText(listOf(TextSpan("hello", null)), listOf(box), TextStyle.Defaults.copy(face = face))
        assertEquals(box.y + face.ascent, lines.single().glyphs.first().position.y, 1E-6)
    }

    @Test
    fun newlineForcesABreak() {
        val lines = typesetText(listOf(TextSpan("a\nb", null)), listOf(Rectangle(0.0, 0.0, 300.0, 300.0)),
            TextStyle.Defaults.copy(face = face()))
        assertEquals(2, lines.size)
    }

    @Test
    fun textOverflowsIntoTheNextBox() {
        val boxes = listOf(Rectangle(0.0, 0.0, 200.0, 40.0), Rectangle(300.0, 0.0, 200.0, 400.0))
        val lines = typesetText(listOf(TextSpan(text, null)), boxes, TextStyle.Defaults.copy(face = face()))
        val boxIndices = lines.map { it.boxIndex }
        assertEquals(boxIndices.sorted(), boxIndices)
        assertTrue(0 in boxIndices && 1 in boxIndices)
    }

    @Test
    fun horizontalAlignmentShiftsLinesRigidly() {
        val box = Rectangle(0.0, 0.0, 220.0, 400.0)
        val left = typesetText(listOf(TextSpan(text, null)), listOf(box), TextStyle.Defaults.copy(face = face()))
        val right = typesetText(listOf(TextSpan(text, null)), listOf(box), TextStyle.Defaults.copy(face = face(), horizontalAlign = 1.0))
        assertEquals(left.size, right.size)
        for ((l, r) in left.zip(right)) {
            val shift = r.glyphs.first().position.x - l.glyphs.first().position.x
            assertTrue(shift >= 0.0)
            for ((lg, rg) in l.glyphs.zip(r.glyphs)) {
                assertEquals(shift, rg.position.x - lg.position.x, 1E-6)
            }
        }
    }

    private fun firstX(spans: List<TextSpan>, direction: Direction, fill: ColorRGBa) =
        typesetText(spans, listOf(Rectangle(0.0, 0.0, 400.0, 400.0)), TextStyle.Defaults.copy(face = face(), direction = direction))
            .single().glyphs.first { it.style.fill == fill }.position.x

    @Test
    fun rightToLeftParagraphOrdersRightToLeftWords() {
        val spans = listOf(TextSpan("مرحبا", TextStyle(fill = ColorRGBa.RED)), TextSpan(" عالم", TextStyle(fill = ColorRGBa.BLUE)))
        assertTrue(firstX(spans, Direction.RIGHT_TO_LEFT, ColorRGBa.RED) > firstX(spans, Direction.RIGHT_TO_LEFT, ColorRGBa.BLUE))
    }

    @Test
    fun leftToRightRunKeepsItsOrderInRightToLeftParagraph() {
        val spans = listOf(TextSpan("first", TextStyle(fill = ColorRGBa.RED)), TextSpan(" second", TextStyle(fill = ColorRGBa.BLUE)))
        assertTrue(firstX(spans, Direction.RIGHT_TO_LEFT, ColorRGBa.RED) < firstX(spans, Direction.RIGHT_TO_LEFT, ColorRGBa.BLUE))
    }

    @Test
    fun typesetLineAppliesBaselineShift() {
        val face = face()
        val plain = typesetLine(face, "a", Vector2(0.0, 100.0), TextStyle.Defaults)
        val shifted = typesetLine(face, "a", Vector2(0.0, 100.0), TextStyle.Defaults.copy(baselineShiftInEm = 0.5))
        assertEquals(100.0, plain.single().position.y, 1E-6)
        assertEquals(100.0 - 0.5 * face.height, shifted.single().position.y, 1E-6)
    }

    @Test
    fun typesetLineOrdersWordsBidirectionally() {
        val face = face()
        val rtl = TextStyle.Defaults.copy(direction = Direction.RIGHT_TO_LEFT)
        val a = face.glyphForCharacter('a').index
        val b = face.glyphForCharacter('b').index

        // in a right-to-left paragraph the first word is placed rightmost, a left-to-right word keeps its glyph order
        val glyphs = typesetLine(face, "ab مرحبا", Vector2.ZERO, rtl)
        val ax = glyphs.single { it.glyphIndex == a }.position.x
        val bx = glyphs.single { it.glyphIndex == b }.position.x
        assertTrue(ax < bx)
        assertTrue(glyphs.filter { it.glyphIndex != a && it.glyphIndex != b }.all { it.position.x < ax })
    }

    @Test
    fun softHyphenIsInvisibleWithoutBreak() {
        val face = face()
        val lines = typesetText(listOf(TextSpan("hy\u00ADphen", null)), listOf(Rectangle(0.0, 0.0, 400.0, 400.0)),
            TextStyle.Defaults.copy(face = face))
        val glyphs = lines.single().glyphs
        assertEquals(6, glyphs.size)
        assertTrue(glyphs.none { it.glyphIndex == face.glyphForCharacter('-').index })
    }

    @Test
    fun lineBreaksAtSoftHyphenShowAHyphen() {
        val face = face()
        val hyphen = face.glyphForCharacter('-').index
        val advance = face.glyphForCharacter('h').advanceWidth()
        // the box fits "hyphen-" but not "hyphenation"
        val box = Rectangle(0.0, 0.0, advance * 8.5, 400.0)
        val lines = typesetText(listOf(TextSpan("hy\u00ADphen\u00ADation", null)), listOf(box),
            TextStyle.Defaults.copy(face = face))
        assertEquals(2, lines.size)
        assertEquals(hyphen, lines[0].glyphs.last().glyphIndex)
        assertEquals(7, lines[0].glyphs.size)
        assertEquals(5, lines[1].glyphs.size)
        assertTrue(lines.flatMap { it.glyphs }.all { it.position.x + advance <= box.width + 1E-6 })
    }

    @Test
    fun hyphenatedTextStaysInsideNarrowBoxes() {
        val face = face()
        val hyphen = face.glyphForCharacter('-').index
        val box = Rectangle(0.0, 0.0, 160.0, 2000.0)
        val text = hyphenate("Typographers appreciate automatic hyphenation of extraordinarily long words.", "en")
        val lines = typesetText(listOf(TextSpan(text, null)), listOf(box), TextStyle.Defaults.copy(face = face))
        assertTrue(lines.any { it.glyphs.last().glyphIndex == hyphen })
        val advance = face.glyphForCharacter('h').advanceWidth()
        assertTrue(lines.flatMap { it.glyphs }.all { it.position.x + advance <= box.width + 1E-6 })
    }

    @Test
    fun languageHyphenatesAutomatically() {
        val face = face()
        val hyphen = face.glyphForCharacter('-').index
        val advance = face.glyphForCharacter('h').advanceWidth()
        val box = Rectangle(0.0, 0.0, advance * 8.5, 400.0)
        val style = TextStyle.Defaults.copy(face = face)

        // without a language "hyphenation" does not fit and overflows the box
        val plain = typesetText(listOf(TextSpan("hyphenation", null)), listOf(box), style)
        assertEquals(1, plain.size)

        val hyphenated = typesetText(listOf(TextSpan("hyphenation", null)), listOf(box), style.copy(language = "en"))
        assertEquals(2, hyphenated.size)
        assertEquals(hyphen, hyphenated[0].glyphs.last().glyphIndex)
        assertEquals(12, hyphenated.sumOf { it.glyphs.size })

        // hyphenation can be disabled
        val disabled = typesetText(listOf(TextSpan("hyphenation", null)), listOf(box), style.copy(language = "en", hyphenate = false))
        assertEquals(1, disabled.size)
    }

    @Test
    fun softHyphensTakePrecedenceOverAutomaticHyphenation() {
        val face = face()
        val advance = face.glyphForCharacter('h').advanceWidth()
        val box = Rectangle(0.0, 0.0, advance * 8.5, 400.0)
        // the patterns would allow "hy-phen-ation", the soft hyphen only allows "hyphena-tion"
        val lines = typesetText(listOf(TextSpan("hyphena\u00ADtion", null)), listOf(box),
            TextStyle.Defaults.copy(face = face, language = "en"))
        assertEquals(listOf(8, 4), lines.map { it.glyphs.size })
    }
}
