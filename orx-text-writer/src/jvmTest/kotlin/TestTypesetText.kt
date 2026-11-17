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
import org.openrndr.extra.textwriter.LineGlue
import org.openrndr.extra.textwriter.shapeParagraph
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
    fun sizeInEmPlacesLinesLikeTheFaceAtThatSize() {
        val base = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf")
        val box = Rectangle(0.0, 50.0, 2000.0, 800.0)
        fun lines(style: TextStyle) = typesetText(listOf(TextSpan("simplex\nsimplex", null)), listOf(box), TextStyle.Defaults.copy(face = style.face, sizeInEm = style.sizeInEm))
        val big = lines(TextStyle.Defaults.copy(face = base.withSize(72.0), sizeInEm = 1.0))
        val scaled = lines(TextStyle.Defaults.copy(face = base.withSize(16.0), sizeInEm = 4.5))
        assertEquals(2, scaled.size)
        // the first baseline and the distance between the lines, within the rounding of the metrics of the sizes
        assertEquals(big[0].glyphs.first().position.y, scaled[0].glyphs.first().position.y, 1.0)
        assertEquals(big[1].glyphs.first().position.y - big[0].glyphs.first().position.y,
            scaled[1].glyphs.first().position.y - scaled[0].glyphs.first().position.y, 2.0)
        // and the same advances, within the precision of the advances at the smaller size
        for ((a, b) in big[0].glyphs.zip(scaled[0].glyphs)) assertEquals(a.position.x, b.position.x, 0.5)
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
    fun textThatDoesNotFitInTheBoxesIsDiscarded() {
        val face = face()
        // two lines high, the text takes more
        val box = Rectangle(0.0, 0.0, 200.0, 60.0)
        for (verticalAlign in listOf(0.0, 0.5, 1.0)) {
            val lines = typesetText(listOf(TextSpan(text.repeat(3), null)), listOf(box), TextStyle.Defaults.copy(face = face, verticalAlign = verticalAlign))
            val baselines = lines.map { it.glyphs.first().position.y }
            assertTrue(lines.size in 1..3, "${lines.size} lines")
            // no line is placed over another one at the start of the box
            assertEquals(baselines.sorted(), baselines)
            assertEquals(baselines.size, baselines.distinct().size, "$baselines")
            // all within the box, also when aligned to its bottom
            assertTrue(baselines.first() - face.ascent >= box.y - 1E-6 && baselines.last() - face.descent <= box.y + box.height + 1E-6,
                "align $verticalAlign: $baselines")
        }
        // with several boxes, the last box is not filled from its start again
        val boxes = listOf(Rectangle(0.0, 0.0, 200.0, 60.0), Rectangle(300.0, 0.0, 200.0, 60.0))
        val lines = typesetText(listOf(TextSpan(text.repeat(6), null)), boxes, TextStyle.Defaults.copy(face = face))
        val last = lines.filter { it.boxIndex == 1 }.map { it.glyphs.first().position.y }
        assertTrue(last.isNotEmpty())
        assertEquals(last.size, last.distinct().size, "$last")
        assertEquals(lines.size, lines.filter { it.boxIndex == 0 }.size + last.size)
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

    @Test
    fun spacingWidensText() {
        val face = face()
        val plain = typesetLine(face, "ab cd", Vector2.ZERO)
        val spaced = typesetLine(face, "ab cd", Vector2.ZERO, TextStyle.Defaults.copy(characterSpacingInEm = 0.5))
        val words = typesetLine(face, "ab cd", Vector2.ZERO, TextStyle.Defaults.copy(wordSpacingInEm = 1.0))
        // every character moves by half an em for every character before it, the space included
        assertEquals(plain.map { it.position.x }.zip(listOf(0, 1, 3, 4)).map { (x, n) -> x + n * 12.0 },
            spaced.map { it.position.x })
        // the space between the words is an em wider
        assertEquals(plain.map { it.position.x }.zip(listOf(0, 0, 1, 1)).map { (x, n) -> x + n * 24.0 },
            words.map { it.position.x })
    }

    private val description = "Maps every element of a list with an expression, like mapIndexed: the expression reads " +
            "the element as it, its index as i and the number of elements as n, and can build values with functions " +
            "like rectangle(x, y, width, height) or vec2(x, y) for every element"

    @Test
    fun raggedTextKeepsItsSpacesAndLeavesNoWordAloneOnItsLastLine() {
        val face = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(13.0)
        val space = face.glyphForCharacter(' ').advanceWidth()
        val advance = face.glyphForCharacter('m').advanceWidth()
        for (width in listOf(220.0, 260.0, 300.0)) {
            val box = Rectangle(0.0, 0.0, width, 2000.0)
            val lines = typesetText(listOf(TextSpan(description, null)), listOf(box), TextStyle.Defaults.copy(face = face))
            for (line in lines) {
                // a monospaced font: every glyph is an advance after the one before, every space as wide as it is
                val xs = line.glyphs.map { it.position.x }
                for ((a, b) in xs.zipWithNext()) {
                    val step = b - a
                    assertTrue(kotlin.math.abs(step - advance) < 0.01 || kotlin.math.abs(step - advance - space) < 0.02, "width $width: a step of $step")
                }
                assertTrue(xs.last() + advance <= width + 0.05, "width $width: a line past the width")
            }
            val last = lines.last().glyphs.let { it.last().position.x + advance - it.first().position.x }
            assertTrue(last >= 0.25 * width, "width $width: a last line of $last")
        }
    }

    @Test
    fun justifiedTextIsBrokenWithStretchableSpaces() {
        val face = face()
        val justified = shapeParagraph(listOf(TextSpan("one two three", null)), TextStyle.Defaults.copy(face = face, justify = true), null)
        val glues = justified.items.filterIsInstance<LineGlue>().dropLast(1)
        assertEquals(2, glues.size)
        assertTrue(glues.all { it.width > 0.0 && it.stretch > 0.0 && it.shrink > 0.0 })
        // ragged text breaks after glue that stretches and takes the stretch back after the break
        val ragged = shapeParagraph(listOf(TextSpan("one two three", null)), TextStyle.Defaults.copy(face = face), null)
        val raggedGlues = ragged.items.filterIsInstance<LineGlue>().dropLast(1)
        assertEquals(listOf(0.0, glues[0].width, 0.0, glues[0].width), raggedGlues.map { it.width })
        assertEquals(0.0, raggedGlues.sumOf { it.stretch }, 1E-9)
    }
}
