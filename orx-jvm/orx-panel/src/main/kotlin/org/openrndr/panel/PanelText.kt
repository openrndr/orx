package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face
import org.openrndr.extra.textwriter.FontFaces
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.extra.textwriter.typesetText
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontWeight
import org.openrndr.panel.style.fontSize
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.round

/**
 * Text that is shaped in a face, with its glyphs placed from the origin on the baseline of the first line
 * @property width the width of the widest line
 * @property height the height from the ascent of the first line to the descent of the last line
 * @property lines the number of lines
 */
class ShapedText internal constructor(
    val text: String,
    val face: Face,
    val glyphs: List<TypesetGlyph>,
    val width: Double,
    val height: Double,
    val lines: Int
) {
    val ascent: Double get() = face.ascent
    val descent: Double get() = face.descent
}

/**
 * Text drawn from the outlines of its glyphs with the boundary drawer of the program, Windfoil by default, instead of
 * from textures of glyphs. Text is crisp at any scale and in any transform. The work is cached at every step:
 *
 * - faces are loaded once per font, weight and size, by [FontFaces], shared with compositions; text is shaped in one
 *   face per font and weight and its glyphs are scaled to the size of the text
 * - text is shaped once per face and string, kept for the most recently used [maxShapedTexts] strings; the shaped
 *   text gives both the measures that layout uses and the glyphs that are drawn, so they agree
 * - the outline of every glyph is uploaded to the GPU once, for all sizes, in a single glyph map shared by all elements
 * - the draw commands of every text an element draws are prepared once in a buffer of their own, in the coordinates
 *   of the element; drawing them again, also when the element moved because it scrolled, only draws the buffer.
 *   Buffers that were not drawn in a redraw are released after it, see [releaseUnused]
 */
class PanelText internal constructor(private val fontManager: FontManager) {
    /** the most strings whose shaping is kept */
    var maxShapedTexts = 2000

    /** the size of the faces that text is shaped in, glyphs are scaled to the size of the text */
    private val referenceSize = 16.0

    /** the font file and weight of every face, for its reference face */
    private val fonts = java.util.IdentityHashMap<Face, Pair<String, Int>>()

    /** the face that [face] is shaped in, one per font and weight, and the size of [face] in its em */
    private fun reference(face: Face): Pair<Face, Double> {
        val (font, weight) = fonts[face] ?: return face to 1.0
        val reference = FontFaces.face(font, referenceSize, weight, opticalSize = null)
        return reference to face.sizeInPoints / referenceSize
    }

    private val shaped = object : LinkedHashMap<Triple<Face, String, Double?>, ShapedText>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Triple<Face, String, Double?>, ShapedText>?) =
            size > maxShapedTexts
    }

    private var boundaryDrawer: BoundaryDrawer? = null
    private var glyphMap: BoundaryGlyphMap? = null

    /** draw commands prepared in a buffer, for the texts with the same [signature] */
    private class Prepared(var buffer: VertexBuffer, var command: BoundaryDrawCommand, var signature: Any, var used: Boolean)

    private val prepared = HashMap<Any, HashMap<String, Prepared>>()

    /** the face of [style]: its font family, weight, style and size */
    fun face(style: StyleSheet): Face {
        val url = fontManager.resolve(style) ?: "cp:fonts/Roboto-Regular.ttf"
        val size = (style.fontSize as? LinearDimension.PX)?.value ?: 16.0
        // the optical size of a variable font follows the reference size, so that glyphs are shaped once for all sizes
        return FontFaces.face(url, size, style.fontWeight, opticalSize = null).also { fonts[it] = url to style.fontWeight }
    }

    /** [text] shaped as a single line in the face of [style] */
    fun line(style: StyleSheet, text: String): ShapedText = line(face(style), text)

    /** [text] shaped as a single line in [face] */
    fun line(face: Face, text: String): ShapedText = shaped.getOrPut(Triple(face, text, null)) {
        val (reference, sizeInEm) = reference(face)
        val glyphs = if (text.isEmpty()) emptyList() else
            typesetLine(reference, text, Vector2.ZERO, TextStyle.Defaults.copy(sizeInEm = sizeInEm))
        val width = glyphs.lastOrNull()?.penEnd?.x ?: 0.0
        ShapedText(text, face, glyphs, width, face.ascent - face.descent, 1)
    }

    /**
     * The x of the caret before every character of [text] in a single line in the face of [style], and after the last:
     * `text.length + 1` positions. From the advances of the glyphs when they are one per character, from the widths of
     * the beginnings of the text otherwise, as with ligatures
     */
    fun carets(style: StyleSheet, text: String): DoubleArray {
        val shaped = line(style, text)
        val result = DoubleArray(text.length + 1)
        if (shaped.glyphs.size == text.length && shaped.glyphs.all { it.advance != null }) {
            var x = 0.0
            for (i in text.indices) {
                result[i] = x
                x += shaped.glyphs[i].advance!!.x
            }
            result[text.length] = x
        } else {
            for (i in 0..text.length) result[i] = line(shaped.face, text.substring(0, i)).width
        }
        return result
    }

    /** the width of [text] in a single line in the face of [style] */
    fun width(style: StyleSheet, text: String): Double = line(style, text).width

    /**
     * [text] in the face of [style], broken into lines at most [width] wide; the first baseline is at y = 0
     */
    fun paragraph(style: StyleSheet, text: String, width: Double, horizontalAlign: Double = 0.0): ShapedText {
        val face = face(style)
        // widths are rounded up, so that relayouts at nearly the same width share the shaping
        val w = ceil(width)
        return shaped.getOrPut(Triple(face, text, w + horizontalAlign * 1E-6)) {
            val (reference, sizeInEm) = reference(face)
            val lines = if (text.isEmpty()) emptyList() else typesetText(
                listOf(TextSpan(text, null)),
                // tall enough for a line per character, typesetting prepares for every line that fits
                listOf(Rectangle(0.0, 0.0, w.coerceAtLeast(1.0), (text.length + 2) * (face.ascent - face.descent + face.lineGap))),
                // the line height of the reference face is scaled to the size like the glyphs
                TextStyle.Defaults.copy(
                    face = reference, sizeInEm = sizeInEm, lineHeightInEm = sizeInEm,
                    hyphenate = false, horizontalAlign = horizontalAlign
                )
            )
            val glyphs = lines.flatMap { it.glyphs }
            // the first baseline at 0
            val first = glyphs.firstOrNull()?.position?.y ?: 0.0
            val moved = glyphs.map {
                TypesetGlyph(it.face, it.glyphIndex, it.position - Vector2(0.0, first), it.style, it.advance, it.offset)
            }
            val lineCount = moved.map { it.position.y }.distinct().size.coerceAtLeast(1)
            val last = moved.maxOfOrNull { it.position.y } ?: 0.0
            val textWidth = moved.maxOfOrNull { it.penEnd?.x ?: it.position.x } ?: 0.0
            ShapedText(text, face, moved, textWidth, last + face.ascent - face.descent, lineCount)
        }
    }

    private fun map(drawer: Drawer): Pair<BoundaryDrawer, BoundaryGlyphMap> {
        val d = boundaryDrawer ?: drawer.boundaryDrawer.also { boundaryDrawer = it }
        val m = glyphMap ?: BoundaryGlyphMap(d.createMap()).also { glyphMap = it }
        return d to m
    }

    /**
     * Draws [shaped] at [position], the origin of its first baseline, in [color]. The draw commands are kept for
     * [owner] under [slot] and drawn again as long as the text, its position and its color stay the same.
     */
    fun draw(drawer: Drawer, owner: Any, slot: String, shaped: ShapedText, position: Vector2, color: ColorRGBa) {
        if (shaped.glyphs.isEmpty() || color.alpha <= 0.0) return
        val (boundaryDrawer, glyphMap) = map(drawer)
        // the baseline on a whole pixel, for even edges
        val origin = Vector2(position.x, round(position.y))
        val signature = Triple(shaped, origin, color)
        val slots = prepared.getOrPut(owner) { HashMap() }
        var entry = slots[slot]
        if (entry == null || entry.signature != signature) {
            glyphMap.startBatch()
            val indices = try {
                shaped.glyphs.map { glyphMap.getBoundaryForGlyphIndex(it.face, it.glyphIndex) }
            } finally {
                glyphMap.endBatch()
            }
            val commands = shaped.glyphs.mapIndexed { i, glyph ->
                BoundaryCommand(indices[i], transform { translate(origin.x, origin.y, 0.0) } * glyph.transform(), color, null, 0.0)
            }
            val buffer = entry?.buffer?.takeIf { it.vertexCount >= commands.size }
                ?: run {
                    entry?.buffer?.close()
                    boundaryDrawer.createInstanceBuffer(commands.size.coerceAtLeast(16))
                }
            val command = boundaryDrawer.prepare(glyphMap.map, commands, buffer)
            entry = Prepared(buffer, command, signature, true)
            slots[slot] = entry
        }
        entry.used = true
        boundaryDrawer.draw(drawer, glyphMap.map, entry.command)
    }

    /**
     * Draws [text] in the face of [style] in [box], at [horizontalAlign] and [verticalAlign] in it, from 0 at the left
     * and the top to 1 at the right and the bottom; broken into lines at the width of the box when [wrap] is set
     */
    fun drawInBox(
        drawer: Drawer, owner: Any, slot: String, style: StyleSheet, text: String, box: Rectangle,
        horizontalAlign: Double, verticalAlign: Double, color: ColorRGBa, wrap: Boolean = false
    ) {
        val shaped = if (wrap) paragraph(style, text, box.width, horizontalAlign) else line(style, text)
        val x = if (wrap) box.x else box.x + (box.width - shaped.width) * horizontalAlign
        val y = box.y + (box.height - shaped.height) * verticalAlign + shaped.ascent
        draw(drawer, owner, slot, shaped, Vector2(x, y), color)
    }

    /** draws a single line of [text] in the face of [style], see [draw] */
    fun drawLine(drawer: Drawer, owner: Any, slot: String, style: StyleSheet, text: String, x: Double, y: Double, color: ColorRGBa) =
        draw(drawer, owner, slot, line(style, text), Vector2(x, y), color)

    /** releases the buffers of the texts that were not drawn since the last call, after a redraw */
    fun releaseUnused() {
        val owners = prepared.entries.iterator()
        while (owners.hasNext()) {
            val (_, slots) = owners.next()
            val entries = slots.entries.iterator()
            while (entries.hasNext()) {
                val entry = entries.next().value
                if (!entry.used) {
                    entry.buffer.close()
                    entries.remove()
                } else {
                    entry.used = false
                }
            }
            if (slots.isEmpty()) owners.remove()
        }
    }

    /** the number of texts whose draw commands are kept, for tests */
    internal val preparedCount: Int get() = prepared.values.sumOf { it.size }

    /** the number of shaped strings that are kept, for tests */
    internal val shapedCount: Int get() = shaped.size
}
