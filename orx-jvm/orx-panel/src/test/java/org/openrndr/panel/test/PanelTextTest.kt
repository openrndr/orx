package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.elements.div
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontSize
import org.openrndr.panel.style.px
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PanelTextTest : PanelTestFixture() {
    @Test
    fun `text is measured like the glyph textures measured it, and its work is cached`() {
        val cm = panel { layout { div { } } }
        val text = cm.text
        val style = StyleSheet().apply { fontSize = 16.px }
        val writer = TextWriter(null).apply { drawStyle.fontMap = cm.fontManager.font(style) }
        for (sample in listOf("Hello world", "radius", "0.250", "Randomize")) {
            val outline = text.width(style, sample)
            val texture = writer.textWidth(sample)
            println("WIDTH '$sample' outline=$outline texture=$texture")
            assertEquals(texture, outline, texture * 0.08)
        }
        assertSame(text.line(style, "radius"), text.line(style, "radius"))
        val carets = text.carets(style, "radius")
        assertEquals(7, carets.size)
        assertEquals(text.width(style, "radius"), carets.last(), 1E-6)
        assertTrue(carets.toList().zipWithNext().all { (a, b) -> b > a })

        // drawing the same text again keeps its prepared commands, texts that are not drawn are released
        val owner = Any()
        program.drawer.isolated {
            text.drawLine(program.drawer, owner, "label", style, "radius", 10.0, 20.0, ColorRGBa.WHITE)
            text.drawLine(program.drawer, owner, "label", style, "radius", 10.0, 20.0, ColorRGBa.WHITE)
        }
        assertEquals(1, text.preparedCount)
        text.releaseUnused()
        assertEquals(1, text.preparedCount)
        text.releaseUnused()
        assertEquals(0, text.preparedCount)
        val paragraph = text.paragraph(style, "a longer text that has to be broken into lines", 80.0)
        assertTrue(paragraph.lines > 2)
        assertTrue(paragraph.width <= 80.0 + 1E-6)
        // lines are a line height apart at every size
        val small = StyleSheet().apply { fontSize = 10.px }
        val smallParagraph = text.paragraph(small, "a longer text that has to be broken into lines", 50.0)
        val face = text.face(small)
        val baselines = smallParagraph.glyphs.map { it.position.y }.distinct().sorted()
        assertEquals(face.ascent - face.descent + face.lineGap, baselines[1] - baselines[0], 0.5)
        // sizes share their glyphs: text at another size is the same text scaled
        assertEquals(text.width(style, "radius") * 10.0 / 16.0, text.width(small, "radius"), 0.5)
    }
}
