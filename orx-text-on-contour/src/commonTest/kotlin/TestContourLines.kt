import org.openrndr.extra.textoncontour.contourLines
import org.openrndr.extra.textwriter.LineBox
import org.openrndr.extra.textwriter.LineGlue
import org.openrndr.extra.textwriter.LineItem
import org.openrndr.extra.textwriter.appendForcedBreak
import kotlin.test.Test
import kotlin.test.assertEquals

class TestContourLines {
    // words of width 10 separated by spaces of width 2, three words fit on a line of width 40
    private fun words(count: Int): List<LineItem<Unit>> {
        val items = mutableListOf<LineItem<Unit>>()
        repeat(count) {
            if (it > 0) items.add(LineGlue(2.0, 1.0, 0.5))
            items.add(LineBox(10.0, Unit))
        }
        items.appendForcedBreak()
        return items
    }

    @Test
    fun verticalAlignmentSelectsContours() {
        val widths = List(5) { 40.0 }
        val items = words(6)
        assertEquals(0, contourLines(items, widths, 0.0).first)
        assertEquals(2, contourLines(items, widths, 0.5).first)
        assertEquals(3, contourLines(items, widths, 1.0).first)
        for (verticalAlign in listOf(0.0, 0.5, 1.0)) {
            assertEquals(2, contourLines(items, widths, verticalAlign).second.size)
        }
    }

    @Test
    fun textIsNotMovedToContoursWhereItDoesNotFit() {
        // the last two contours are too short for a line of three words
        val widths = listOf(40.0, 40.0, 40.0, 20.0, 20.0)
        assertEquals(1, contourLines(words(6), widths, 1.0).first)
    }

    @Test
    fun textThatFillsAllContoursIsNotMoved() {
        val widths = List(3) { 40.0 }
        val (first, lines) = contourLines(words(12), widths, 1.0)
        assertEquals(0, first)
        assertEquals(3, lines.size)
    }
}
