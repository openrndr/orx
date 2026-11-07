package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.elements.div
import org.openrndr.panel.style.StyleSheet
import org.openrndr.extra.textwriter.FontFaces
import org.openrndr.extra.textwriter.Fonts
import org.openrndr.panel.style.fontFamily
import org.openrndr.panel.style.fontSize
import org.openrndr.panel.style.fontWeight
import org.openrndr.panel.style.px
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PanelTextTest : PanelTestFixture() {
    @Test
    fun `fonts are resolved by family, weight and style, and faces are shared`() {
        val cm = panel { layout { div { } } }
        val text = cm.text
        val regular = StyleSheet().apply { fontSize = 16.px }
        val medium = StyleSheet().apply { fontSize = 16.px; fontWeight = 500 }
        val bold = StyleSheet().apply { fontSize = 16.px; fontWeight = 700 }
        assertTrue(cm.fontManager.resolve(regular)!!.endsWith("Roboto-Regular.ttf"))
        assertTrue(cm.fontManager.resolve(medium)!!.endsWith("Roboto-Medium.ttf"))
        // the nearest weight
        assertTrue(cm.fontManager.resolve(bold)!!.endsWith("Roboto-Medium.ttf"))
        assertTrue(text.width(medium, "Randomize") > text.width(regular, "Randomize"))
        // families that are not registered are followed by the next ones and by the default family
        val unknown = StyleSheet().apply { fontSize = 16.px; fontFamily = "Unknown, also unknown" }
        assertTrue(cm.fontManager.resolve(unknown)!!.endsWith("Roboto-Regular.ttf"))
        // the same font at the same size is the same face, for everything that draws text
        assertSame(text.face(regular), text.face(StyleSheet().apply { fontSize = 16.px }))
        assertSame(text.face(regular), FontFaces.face(cm.fontManager.resolve(regular)!!, 16.0, opticalSize = null))
        // families of all programs are found too
        Fonts.register("Everywhere", cm.fontManager.resolve(medium)!!)
        assertTrue(cm.fontManager.resolve(StyleSheet().apply { fontFamily = "Everywhere" })!!.endsWith("Roboto-Medium.ttf"))
        // the registry of names is kept for compatibility
        cm.fontManager.registry["mono"] = cm.fontManager.resolve(medium)!!
        assertTrue(cm.fontManager.resolve("mono")!!.endsWith("Roboto-Medium.ttf"))
    }

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
