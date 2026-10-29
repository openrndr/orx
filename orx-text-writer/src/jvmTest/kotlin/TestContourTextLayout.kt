import org.openrndr.draw.font.loadFace
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.extra.textwriter.ContourBaseline
import org.openrndr.extra.textwriter.ContourOverflow
import org.openrndr.extra.textwriter.GlyphPivot
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.layoutTextOnContour
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestContourTextLayout : AbstractApplicationTestFixture() {
    private fun face() = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0)

    private val line = LineSegment(0.0, 100.0, 1000.0, 100.0).contour

    @Test
    fun straightContourMatchesTypesetLine() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val layout = layoutTextOnContour(listOf(TextSpan("Hello world", null)), ContourBaseline(line), style, 50.0)
        val expected = typesetLine(face, "Hello world", Vector2(50.0, 100.0), style)
        assertEquals(expected.size, layout.glyphs.size)
        for ((placed, glyph) in layout.glyphs.zip(expected)) {
            assertEquals(glyph.position.x, placed.transform.c3r0, 1E-3)
            assertEquals(glyph.position.y, placed.transform.c3r1, 1E-3)
        }
        face.close()
    }

    @Test
    fun middlePivotOnStraightContourMatchesStartPivot() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val spans = listOf(TextSpan("Hello", null))
        val start = layoutTextOnContour(spans, ContourBaseline(line), style, 50.0, pivot = GlyphPivot.START)
        val middle = layoutTextOnContour(spans, ContourBaseline(line), style, 50.0, pivot = GlyphPivot.MIDDLE)
        for ((a, b) in start.glyphs.zip(middle.glyphs)) {
            assertEquals(a.transform.c3r0, b.transform.c3r0, 1E-3)
        }
        face.close()
    }

    @Test
    fun anchorEndsTextAtStartDistance() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val layout = layoutTextOnContour(listOf(TextSpan("Hello", null)), ContourBaseline(line), style, 500.0, anchor = 1.0)
        assertEquals(500.0, layout.end, 1E-9)
        // the advances of the shaper are rounded to 1/64 pixel
        assertEquals(5 * face.glyphForCharacter('H').advanceWidth(), layout.advance, 0.1)
        face.close()
    }

    @Test
    fun hiddenOverflowDropsGlyphsBeyondTheEnds() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val short = LineSegment(0.0, 0.0, 30.0, 0.0).contour
        val spans = listOf(TextSpan("abcdefgh", null))
        val hidden = layoutTextOnContour(spans, ContourBaseline(short), style, -10.0, overflow = ContourOverflow.HIDE, pivot = GlyphPivot.MIDDLE)
        val clamped = layoutTextOnContour(spans, ContourBaseline(short), style, -10.0, overflow = ContourOverflow.CLAMP)
        assertEquals(8, clamped.glyphs.size)
        assertTrue(hidden.glyphs.size in 1 until 8)
        face.close()
    }

    @Test
    fun positiveOffsetMovesTowardsAscenders() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val layout = layoutTextOnContour(listOf(TextSpan("H", null)), ContourBaseline(line, offset = 10.0), style)
        // y points down, the ascenders of text on a left to right contour point up
        assertEquals(90.0, layout.glyphs.first().transform.c3r1, 1E-6)
        face.close()
    }

    @Test
    fun wrappedTextStaysOnClosedContour() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val circle = Circle(0.0, 0.0, 20.0).contour
        val layout = layoutTextOnContour(listOf(TextSpan("a long text that wraps around the circle", null)), ContourBaseline(circle), style)
        for (placed in layout.glyphs) {
            assertEquals(20.0, Vector2(placed.transform.c3r0, placed.transform.c3r1).length, 0.05)
        }
        face.close()
    }

    @Test
    fun shiftsAccumulateFromTheirSpanOn() {
        val face = face()
        val style = TextStyle.Defaults.copy(face = face)
        val spans = listOf(TextSpan("ab", null), TextSpan("cd", null), TextSpan("ef", null))
        val shifts = listOf(Vector2.ZERO, Vector2(10.0, -5.0), Vector2(0.0, 2.0))
        val plain = layoutTextOnContour(spans, ContourBaseline(line), style)
        val shifted = layoutTextOnContour(spans, ContourBaseline(line), style, shifts = shifts)
        val expected = listOf(0.0 to 0.0, 0.0 to 0.0, 10.0 to -5.0, 10.0 to -5.0, 10.0 to -3.0, 10.0 to -3.0)
        for ((index, shift) in expected.withIndex()) {
            assertEquals(plain.glyphs[index].transform.c3r0 + shift.first, shifted.glyphs[index].transform.c3r0, 1E-3)
            assertEquals(plain.glyphs[index].transform.c3r1 + shift.second, shifted.glyphs[index].transform.c3r1, 1E-3)
        }
        assertEquals(plain.advance + 10.0, shifted.advance, 1E-6)
        face.close()
    }
}
