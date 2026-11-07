package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face
import org.openrndr.extra.textwriter.FontFaces
import org.openrndr.extra.textwriter.GlyphMaps
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.extra.textwriter.typesetText
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.panel.style.*
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
 * - the outline of every glyph is uploaded to the GPU once, for all sizes, in the glyph map of the boundary drawer
 *   that all elements, compositions and other text drawers share, see [GlyphMaps]
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

    /** the key of shaped text: a single line when [width] is null, a paragraph otherwise */
    private data class Key(val face: Face, val text: String, val width: Double?, val spacing: Double, val lineHeight: Double?)

    private val shaped = object : LinkedHashMap<Key, ShapedText>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, ShapedText>?) =
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

    /** the letter spacing of [style] in pixels */
    private fun spacing(style: StyleSheet): Double = (style.letterSpacing as? LinearDimension.PX)?.value ?: 0.0

    /** the line height of [style] in pixels, null for the line height of its font */
    private fun lineHeight(style: StyleSheet): Double? {
        val size = (style.fontSize as? LinearDimension.PX)?.value ?: 16.0
        return when (val h = style.lineHeight) {
            is LinearDimension.PX -> h.value
            is LinearDimension.Percent -> h.value / 100.0 * size
            else -> null
        }
    }

    /** [glyphs] moved by [spacing] after every glyph, in lines by their y */
    private fun spaced(glyphs: List<TypesetGlyph>, spacing: Double): List<TypesetGlyph> {
        if (spacing == 0.0) return glyphs
        var line = Double.NaN
        var index = 0
        return glyphs.map {
            if (it.position.y != line) { line = it.position.y; index = 0 }
            val shift = Vector2(spacing * index++, 0.0)
            TypesetGlyph(it.face, it.glyphIndex, it.position + shift, it.style, it.advance, it.offset)
        }
    }

    /** [text] shaped as a single line in the face of [style], with its letter spacing */
    fun line(style: StyleSheet, text: String): ShapedText = line(face(style), text, spacing(style))

    /** [text] shaped as a single line in [face], with [spacing] after every character */
    fun line(face: Face, text: String, spacing: Double = 0.0): ShapedText = shaped.getOrPut(Key(face, text, null, spacing, null)) {
        val (reference, sizeInEm) = reference(face)
        val glyphs = if (text.isEmpty()) emptyList() else
            spaced(typesetLine(reference, text, Vector2.ZERO, TextStyle.Defaults.copy(sizeInEm = sizeInEm)), spacing)
        // white space at the end has no glyphs, but takes room, like the space that was just typed
        val trailing = text.length - text.trimEnd().length
        val width = (glyphs.lastOrNull()?.penEnd?.x ?: 0.0) + (if (glyphs.isEmpty()) 0.0 else spacing) +
                if (trailing > 0) trailing * (spaceAdvance(face, spacing) + spacing) else 0.0
        ShapedText(text, face, glyphs, width, face.ascent - face.descent, 1)
    }

    private val spaceAdvances = HashMap<Pair<Face, Double>, Double>()

    /** the advance of a space in [face], which has no glyph */
    private fun spaceAdvance(face: Face, spacing: Double): Double = spaceAdvances.getOrPut(face to spacing) {
        line(face, "x x", spacing).width - line(face, "xx", spacing).width - spacing
    }

    /**
     * The x of the caret before every character of [text] in a single line in the face of [style], and after the last:
     * `text.length + 1` positions. From the advances of the glyphs when they are one per character, from the widths of
     * the beginnings of the text otherwise, as with ligatures
     */
    fun carets(style: StyleSheet, text: String): DoubleArray {
        val shaped = line(style, text)
        val spacing = spacing(style)
        val result = DoubleArray(text.length + 1)
        if (shaped.glyphs.size == text.length && shaped.glyphs.all { it.advance != null }) {
            var x = 0.0
            for (i in text.indices) {
                result[i] = x
                x += shaped.glyphs[i].advance!!.x + spacing
            }
            result[text.length] = x
        } else if (shaped.glyphs.size == text.count { !it.isWhitespace() } && shaped.glyphs.all { it.advance != null }) {
            // white space has no glyphs: a glyph for every other character, the space advance for white space
            val space = spaceAdvance(shaped.face, spacing) + spacing
            var x = 0.0
            var g = 0
            for (i in text.indices) {
                if (text[i].isWhitespace()) {
                    result[i] = x
                    x += space
                } else {
                    val glyph = shaped.glyphs[g++]
                    result[i] = glyph.position.x - glyph.offset.x
                    x = glyph.penEnd!!.x + spacing
                }
            }
            result[text.length] = x
        } else {
            for (i in 0..text.length) result[i] = line(shaped.face, text.substring(0, i), spacing).width
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
        val spacing = spacing(style)
        val lineHeight = lineHeight(style)
        return shaped.getOrPut(Key(face, text, w + horizontalAlign * 1E-6, spacing, lineHeight)) {
            val (reference, sizeInEm) = reference(face)
            // the line height of the font, or that of the style
            val faceLineHeight = face.ascent - face.descent + face.lineGap
            val lineHeightInEm = if (lineHeight == null) sizeInEm else sizeInEm * lineHeight / faceLineHeight
            val lines = if (text.isEmpty()) emptyList() else typesetText(
                listOf(TextSpan(text, null)),
                // tall enough for a line per character, typesetting prepares for every line that fits
                listOf(Rectangle(0.0, 0.0, w.coerceAtLeast(1.0), (text.length + 2) * (face.ascent - face.descent + face.lineGap))),
                // the line height of the reference face is scaled to the size like the glyphs
                TextStyle.Defaults.copy(
                    face = reference, sizeInEm = sizeInEm, lineHeightInEm = lineHeightInEm,
                    hyphenate = false, horizontalAlign = horizontalAlign
                )
            )
            val glyphs = lines.flatMap { it.glyphs }
            // the first baseline at 0
            val first = glyphs.firstOrNull()?.position?.y ?: 0.0
            val moved = spaced(glyphs.map {
                TypesetGlyph(it.face, it.glyphIndex, it.position - Vector2(0.0, first), it.style, it.advance, it.offset)
            }, spacing)
            val lineCount = moved.map { it.position.y }.distinct().size.coerceAtLeast(1)
            val last = moved.maxOfOrNull { it.position.y } ?: 0.0
            val textWidth = moved.maxOfOrNull { it.penEnd?.x ?: it.position.x } ?: 0.0
            ShapedText(text, face, moved, textWidth, last + face.ascent - face.descent, lineCount)
        }
    }

    /** a run of rich text: [text] in the face and color of [style] */
    class Run(val text: String, val style: StyleSheet)

    /** rich text shaped in lines: [shaped], and the run of every glyph */
    class RichShaped(val shaped: ShapedText, val runOfGlyph: IntArray)

    private data class RichKey(val runs: List<Triple<String, Face, Double>>, val width: Double, val align: Double)

    private val rich = object : LinkedHashMap<RichKey, RichShaped>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<RichKey, RichShaped>?) = size > maxShapedTexts / 4
    }

    /**
     * [runs] of text in their own faces broken into lines at most [width] wide, as one paragraph; the first baseline
     * is at y = 0. The lines are as far apart as the line height of the largest face.
     */
    fun richParagraph(runs: List<Run>, width: Double, horizontalAlign: Double = 0.0): RichShaped {
        val faces = runs.map { face(it.style) }
        val w = ceil(width)
        return rich.getOrPut(RichKey(runs.mapIndexed { i, r -> Triple(r.text, faces[i], spacing(r.style)) }, w, horizontalAlign)) {
            if (runs.isEmpty() || runs.all { it.text.isEmpty() }) {
                return@getOrPut RichShaped(ShapedText("", faces.firstOrNull() ?: face(StyleSheet()), emptyList(), 0.0, 0.0, 1), IntArray(0))
            }
            // every run has a fill of its own, its alpha tells the glyphs of the run apart
            val spans = runs.mapIndexed { i, run ->
                val (reference, sizeInEm) = reference(faces[i])
                TextSpan(run.text, TextStyle(face = reference, sizeInEm = sizeInEm, fill = ColorRGBa(0.0, 0.0, 0.0, 1.0 - (i + 1) * 1E-6)))
            }
            val largest = faces.indices.maxBy { faces[it].sizeInPoints }
            val (reference, sizeInEm) = reference(faces[largest])
            val lineHeight = (faces[largest].ascent - faces[largest].descent + faces[largest].lineGap)
            val lines = typesetText(
                spans,
                listOf(Rectangle(0.0, 0.0, w.coerceAtLeast(1.0), (runs.sumOf { it.text.length } + 2) * lineHeight)),
                TextStyle.Defaults.copy(face = reference, sizeInEm = sizeInEm, lineHeightInEm = sizeInEm, hyphenate = false, horizontalAlign = horizontalAlign)
            )
            val glyphs = lines.flatMap { it.glyphs }
            val first = glyphs.firstOrNull()?.position?.y ?: 0.0
            val moved = glyphs.map { TypesetGlyph(it.face, it.glyphIndex, it.position - Vector2(0.0, first), it.style, it.advance, it.offset) }
            val runOfGlyph = IntArray(moved.size) { g ->
                val alpha = moved[g].style.fill?.alpha ?: 1.0
                kotlin.math.round((1.0 - alpha) / 1E-6).toInt().minus(1).coerceIn(0, runs.size - 1)
            }
            val last = moved.maxOfOrNull { it.position.y } ?: 0.0
            val textWidth = moved.maxOfOrNull { it.penEnd?.x ?: it.position.x } ?: 0.0
            val face = faces[largest]
            RichShaped(ShapedText(runs.joinToString("") { it.text }, face, moved, textWidth, last + face.ascent - face.descent,
                moved.map { it.position.y }.distinct().size.coerceAtLeast(1)), runOfGlyph)
        }
    }

    private fun map(drawer: Drawer): Pair<BoundaryDrawer, BoundaryGlyphMap> {
        val d = boundaryDrawer ?: drawer.boundaryDrawer.also { boundaryDrawer = it }
        // the glyph map is shared with compositions and everything else that draws with the boundary drawer
        val m = glyphMap ?: GlyphMaps.of(d).also { glyphMap = it }
        return d to m
    }

    /**
     * Draws [shaped] at [position], the origin of its first baseline, in [color]. The draw commands are kept for
     * [owner] under [slot] and drawn again as long as the text, its position and its color stay the same.
     */
    fun draw(
        drawer: Drawer, owner: Any, slot: String, shaped: ShapedText, position: Vector2, color: ColorRGBa,
        decoration: TextDecoration = TextDecoration.NONE, glyphColors: List<ColorRGBa>? = null
    ) {
        if (shaped.glyphs.isEmpty() || (color.alpha <= 0.0 && glyphColors == null)) return
        if (!decoration.isNone) drawDecoration(drawer, shaped, Vector2(position.x, round(position.y)), color, decoration)
        val (boundaryDrawer, glyphMap) = map(drawer)
        // the baseline on a whole pixel, for even edges
        val origin = Vector2(position.x, round(position.y))
        val signature = Triple(shaped, origin, glyphColors ?: color)
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
                BoundaryCommand(indices[i], transform { translate(origin.x, origin.y, 0.0) } * glyph.transform(), glyphColors?.get(i) ?: color, null, 0.0)
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
     * Draws the lines of [decoration] of the lines of [shaped] at [origin], the origin of its first baseline; only
     * under, over and through [only] of its glyphs when it is given
     */
    internal fun drawDecoration(
        drawer: Drawer, shaped: ShapedText, origin: Vector2, color: ColorRGBa, decoration: TextDecoration,
        only: Set<Int>? = null
    ) {
        val size = shaped.face.sizeInPoints
        val thickness = maxOf(1.0, round(size / 14.0))
        drawer.isolated {
            drawer.fill = color
            drawer.stroke = null
            val selected = if (only == null) shaped.glyphs else shaped.glyphs.filterIndexed { i, _ -> i in only }
            for ((y, glyphs) in selected.groupBy { it.position.y }) {
                val x0 = glyphs.minOf { it.position.x }
                val x1 = glyphs.maxOf { it.penEnd?.x ?: it.position.x }
                fun line(offset: Double) = drawer.rectangle(origin.x + x0, origin.y + y + offset - thickness / 2.0, x1 - x0, thickness)
                if (decoration.underline) line(size * 0.12)
                if (decoration.overline) line(-shaped.ascent)
                if (decoration.lineThrough) line(-shaped.ascent * 0.32)
            }
        }
    }

    /** [text] cut at its end, with an ellipsis, so that it fits in [width], as a single line in the face of [style] */
    fun ellipsized(style: StyleSheet, text: String, width: Double): String {
        if (width(style, text) <= width) return text
        var lo = 0
        var hi = text.length
        // the longest beginning that fits with the ellipsis
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (width(style, text.substring(0, mid).trimEnd() + "…") <= width) lo = mid else hi = mid - 1
        }
        return text.substring(0, lo).trimEnd() + "…"
    }

    /**
     * Draws [text] in the face of [style] in [box], at [horizontalAlign] and [verticalAlign] in it, from 0 at the left
     * and the top to 1 at the right and the bottom; broken into lines at the width of the box when [wrap] is set and
     * the white space of the style wraps, cut with an ellipsis when it is a single line that does not fit and the text
     * overflow of the style is ellipsis, with the decoration of the style
     */
    fun drawInBox(
        drawer: Drawer, owner: Any, slot: String, style: StyleSheet, text: String, box: Rectangle,
        horizontalAlign: Double, verticalAlign: Double, color: ColorRGBa, wrap: Boolean = false
    ) {
        val wraps = wrap && style.whiteSpace == WhiteSpace.NORMAL
        val shaped = when {
            wraps -> paragraph(style, text, box.width, horizontalAlign)
            // lines that are not wrapped, broken at their line breaks
            '\n' in text -> paragraph(style, text, text.lines().maxOf { width(style, it) } + 1.0, horizontalAlign)
            style.textOverflow == TextOverflow.ELLIPSIS -> line(style, ellipsized(style, text, box.width))
            else -> line(style, text)
        }
        val x = if (wraps) box.x else box.x + (box.width - shaped.width) * horizontalAlign
        val y = box.y + (box.height - shaped.height) * verticalAlign + shaped.ascent
        draw(drawer, owner, slot, shaped, Vector2(x, y), color, style.textDecoration)
    }

    /** draws a single line of [text] in the face of [style], with its decoration, see [draw] */
    fun drawLine(drawer: Drawer, owner: Any, slot: String, style: StyleSheet, text: String, x: Double, y: Double, color: ColorRGBa) =
        draw(drawer, owner, slot, line(style, text), Vector2(x, y), color, style.textDecoration)

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
