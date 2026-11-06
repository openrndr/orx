package org.openrndr.extra.composition

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.extra.textwriter.ContourBaseline
import org.openrndr.extra.textwriter.ContourOverflow
import org.openrndr.extra.textwriter.ContourTextLayout
import org.openrndr.extra.textwriter.GlyphPivot
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.layoutTextOnContour
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * Draws the text of [TextNode]s with a [SlugTextDrawer]
 */
internal object CompositionText {
    // the slug drawer keeps its glyphs in textures, it is created once it is needed
    private val slugTextDrawer by lazy { SlugTextDrawer() }

    private val failedFonts = mutableSetOf<String>()

    // text is drawn in a single color, gradients and other paints are drawn in the color that stands in for them
    private fun Paint.color(opacity: Double, style: Style): ColorRGBa? = when (this) {
        is Paint.RGB -> value.opacify(opacity)
        is Paint.Server -> value?.copy(alpha = 1.0)?.opacify(opacity)
        is Paint.CurrentColor -> currentColor(style)?.opacify(opacity)
        else -> null
    }

    /** the opacity of [node], the opacity of the groups it is in is applied by their layers */
    private fun opacity(node: CompositionNode): Double = (node.style.opacity as? Numeric.Rational)?.value ?: 1.0

    /**
     * The face of the font of [style] for [node], null when there is no font or it cannot be loaded. The font faces of
     * the groups the node is in are used before the registered fonts, the first source that loads is used.
     */
    private fun face(style: Style, node: CompositionNode): Face? {
        val fontSize = (style.fontSize as? Length.Pixels)?.value ?: 16.0
        val faces = generateSequence(node) { it.parent }.filterIsInstance<GroupNode>().flatMap { it.fontFaces }.toList()
        val candidates = CompositionFonts.candidates(style.fontFamily.value, style.fontWeight.value, style.fontStyle.value, faces)
        for (file in candidates) {
            if (file in failedFonts) continue
            try {
                return variations(loadFace(file).withSize(fontSize), style)
            } catch (e: Throwable) {
                failedFonts.add(file)
                logger.warn { "could not load font '${file.take(80)}': ${e.message}" }
            }
        }
        return null
    }

    /**
     * [face] with the axes of [style] when it is a variable font: its font-variation-settings, and its weight on the
     * weight axis when the settings do not give it, as browsers do
     */
    private fun variations(face: Face, style: Style): Face {
        if (!face.isVariable) return face
        val master = face.master.copy()
        // the axes of faces are named by their names, registered axes are given by their tags in CSS
        fun axis(tag: String) = registeredAxes[tag]?.takeIf { it in master } ?: tag.takeIf { it in master }
        val settings = style.fontVariationSettings.value.mapNotNull { (tag, value) -> axis(tag)?.let { it to value } }.toMap()
        for ((name, value) in settings) master[name] = value
        // the weight follows font-weight and the optical size follows the font size, as in browsers
        axis("wght")?.takeIf { it !in settings }?.let { master[it] = style.fontWeight.value.toDouble() }
        axis("opsz")?.takeIf { it !in settings }?.let { master[it] = fontSize(style) }
        return if (master == face.master) face else face.withMaster(master)
    }

    /** the names of the registered axes of variable fonts by their tags */
    private val registeredAxes = mapOf(
        "wght" to "Weight", "wdth" to "Width", "opsz" to "OpticalSize", "ital" to "Italic", "slnt" to "Slant"
    )

    /**
     * How the text of [node] is adjusted to the textLength of the text element or tspan it is in: [spacing] is added
     * after every character, the advances are scaled by [factor]. [last] is the last run of the adjusted text, the
     * spacing is not added after its last character.
     */
    private class Adjustment(val spacing: Double, val factor: Double, val last: TextNode?)

    private val noAdjustment = Adjustment(0.0, 1.0, null)

    private fun adjustment(node: TextNode): Adjustment {
        var current: CompositionNode? = node.parent
        while (current is GroupNode) {
            val length = (current.style.textLength.value as? Length.Pixels)?.value
            if (length != null) {
                val runs = textRuns(current)?.filter { it.contour == null } ?: return noAdjustment
                // the natural width of the text and its number of characters
                val natural = runs.sumOf { run -> layoutRun(run, Vector2.ZERO, adjust = false)?.let { it.end.x - it.origin.x } ?: 0.0 }
                val characters = runs.sumOf { run -> run.text.count { !it.isLowSurrogate() } }
                if (natural <= 0.0 || characters == 0) return noAdjustment
                return when (current.style.lengthAdjust) {
                    LengthAdjust.SPACING -> if (characters < 2) noAdjustment else
                        Adjustment((length - natural) / (characters - 1), 1.0, runs.last())
                    LengthAdjust.SPACING_AND_GLYPHS -> Adjustment(0.0, length / natural, null)
                }
            }
            if (current.textElement) break
            current = current.parent
        }
        return noAdjustment
    }

    /** the paint servers of the fill and stroke of [style], null when neither is one */
    private fun servers(unresolved: Style): Pair<Paint.Server?, Paint.Server?>? {
        val style = resolveContextPaints(unresolved)
        val fill = style.fill as? Paint.Server
        val stroke = style.stroke as? Paint.Server
        return if (fill == null && stroke == null) null else Pair(fill, stroke)
    }

    /**
     * The fill and stroke of the text of [node]. Paints that are drawn with a paint style are white, the paint style
     * replaces the color and keeps the alpha.
     */
    private fun textStyle(node: TextNode, unresolved: Style, adjust: Boolean = true): TextStyle {
        val style = resolveContextPaints(unresolved)
        val adjustment = if (adjust) adjustment(node) else noAdjustment
        val opacity = opacity(node)
        fun color(paint: Paint, paintOpacity: Double): ColorRGBa? = when (paint) {
            is Paint.Server -> ColorRGBa.WHITE.opacify(paintOpacity)
            else -> paint.color(paintOpacity, style)
        }
        val fill = color(style.fill, opacity * ((style.fillOpacity as? Numeric.Rational)?.value ?: 1.0))
        val stroke = color(style.stroke, opacity * ((style.strokeOpacity as? Numeric.Rational)?.value ?: 1.0))
        val fontSize = fontSize(style)
        return TextStyle.Defaults.copy(
            fill = fill,
            stroke = stroke,
            strokeWeight = if (stroke != null) (style.strokeWeight as? Length.Pixels)?.value else null,
            characterSpacingInEm = (pixels(style.letterSpacing, fontSize) + adjustment.spacing) / fontSize,
            wordSpacingInEm = pixels(style.wordSpacing, fontSize) / fontSize,
            textWidthFactor = adjustment.factor,
            features = style.fontFeatureSettings.value.map { (tag, value) -> ShapeFeature(tag, value.toInt()) }
        )
    }

    private fun fontSize(style: Style) = (style.fontSize as? Length.Pixels)?.value ?: 16.0

    /** [length] in pixels, a percentage is one of [fontSize] */
    private fun pixels(length: Length, fontSize: Double) = when (length) {
        is Length.Pixels -> length.value
        is Length.Percent -> fontSize * length.value / 100.0
    }

    /**
     * How far the alphabetic baseline of [node] is below its position: the offset of its dominant baseline minus the
     * baseline shifts of the node and of the groups it is in
     */
    private fun baselineOffset(node: TextNode, style: Style, face: Face): Double {
        // vertical text is centered on its line, also along paths
        val baseline = if (style.writingMode.vertical && style.dominantBaseline == DominantBaseline.AUTO) DominantBaseline.CENTRAL
            else style.dominantBaseline
        val dominant = baseline.offset(abs(face.ascent), abs(face.descent), face.xHeight)
        var shift = 0.0
        var current: CompositionNode? = node
        while (current != null) {
            val own = current.style
            if (own.properties[AttributeOrPropertyKey.BASELINE_SHIFT] != null) {
                val currentStyle = current.effectiveStyle
                shift += when (val baselineShift = own.baselineShift) {
                    is BaselineShift.Value -> pixels(baselineShift.value, fontSize(currentStyle))
                    else -> {
                        val currentFace = face(currentStyle, current) ?: face
                        val half = (abs(currentFace.ascent) + abs(currentFace.descent)) / 2.0
                        if (baselineShift is BaselineShift.Sub) -half else half
                    }
                }
            }
            current = current.parent
        }
        return dominant - shift
    }

    /**
     * Sets the paint style for text with [style] on [drawer], with paints over the bounds of the text mapped to
     * [bounds]. Paints that can not be drawn, over bounds that are not known or are empty, are left out.
     * @return false when nothing of the text is drawn
     */
    private fun applyPaints(drawer: Drawer, style: Style, bounds: Rectangle?): Boolean {
        val (fillServer, strokeServer) = servers(style) ?: return true
        val emptyBounds = Rectangle(0.0, 0.0, 0.0, 0.0)
        val fillTransform = fillServer?.let { drawer.preparePaint(it, bounds ?: emptyBounds) }
        val strokeTransform = strokeServer?.let { drawer.preparePaint(it, bounds ?: emptyBounds) }
        val fill = if (fillTransform != null) fillServer else null
        val stroke = if (strokeTransform != null) strokeServer else null
        if (fill == null && stroke == null) {
            // the paints of a run with a server have no color to fall back on
            return style.fill !is Paint.Server && style.stroke !is Paint.Server
        }
        val paint = paintStyle(fill, stroke)
        fillTransform?.let { paint.fillTransform = it }
        strokeTransform?.let { paint.strokeTransform = it }
        drawer.shadeStyle = paint
        return true
    }

    /** the height of the lines of text with [style] and [face], like line-height */
    private fun lineHeight(style: Style, face: Face): Double = when (val h = style.lineHeight) {
        is LineHeight.Multiple -> h.value * fontSize(style)
        is LineHeight.Absolute -> h.value
        // the line height of the face, its ascent, descent and line gap
        else -> abs(face.ascent) + abs(face.descent) + abs(face.lineGap)
    }

    /** true when [c] is a character of a script that is written from right to left */
    private fun isRightToLeft(c: Char): Boolean = c.code in 0x0590..0x08FF || c.code in 0xFB1D..0xFDFF || c.code in 0xFE70..0xFEFF

    /**
     * True when the runs of [group] are ordered by the bidirectional algorithm across runs: when its direction is
     * right to left, when it holds characters of scripts that are written right to left, or uses unicode-bidi
     */
    private fun needsBidi(group: GroupNode, runs: List<TextNode>): Boolean =
        group.effectiveStyle.direction == TextDirection.RTL ||
                runs.any { run -> run.text.any(::isRightToLeft) || run.parent?.style?.unicodeBidi != UnicodeBidi.NORMAL ||
                        run.effectiveStyle.direction == TextDirection.RTL }

    /**
     * The runs of a text element arranged in lines, when the text wraps or is bidirectional, null otherwise; they are
     * drawn in place of the runs of the text element.
     *
     * Text with an inline size wraps like SVG 2 text: lines break at spaces and at the line feeds of preserved white
     * space, the first line starts at the position of the text and the next lines follow at the line height, every
     * line is anchored at that position; vertical text wraps into columns. The runs of a line of bidirectional text
     * are ordered by the bidirectional algorithm over the whole line, as browsers do: consecutive runs of left to
     * right text read left to right in right to left text, and the other way around.
     */
    fun arrange(group: GroupNode): List<TextNode>? {
        val runs = textRuns(group)?.takeIf { it.isNotEmpty() && it.all { r -> r.contour == null } } ?: return null
        val groupStyle = group.effectiveStyle
        val vertical = groupStyle.writingMode.vertical
        val inlineSize = (group.style.inlineSize.value as? Length.Pixels)?.value
        val bidi = !vertical && needsBidi(group, runs)
        if (inlineSize == null && !bidi) return null
        val first = runs.first()
        val face = face(groupStyle, first) ?: return null
        val anchor = groupStyle.textAnchor.value
        val rtl = !vertical && groupStyle.direction == TextDirection.RTL

        /** a part of a run that is placed as a whole, with its advance */
        class Segment(val run: TextNode, val text: String) {
            val width: Double by lazy {
                if (text == "\n") 0.0 else {
                    val probe = TextNode(text, null).also { it.parent = run.parent; it.style = run.style }
                    layoutRun(probe, Vector2.ZERO)?.advance ?: 0.0
                }
            }
        }

        // the lines, with their positions: the position of the text and the index of the line, or the positions of the
        // parts of the text that start at a position of their own
        class Line(val position: Vector2, val index: Int, val segments: List<Segment>)
        val lines = mutableListOf<Line>()
        val x = (first.x ?: 0.0) + first.dx
        val y = (first.y ?: 0.0) + first.dy
        if (inlineSize != null) {
            val tokens = runs.flatMap { run -> Regex("\n| +|[^ \n]+").findAll(run.text).map { Segment(run, it.value) } }
            val tokenLines = mutableListOf(mutableListOf<Segment>())
            var width = 0.0
            var pending = mutableListOf<Segment>()
            for (token in tokens) {
                val isSpace = token.text.all { it == ' ' }
                when {
                    token.text == "\n" -> { tokenLines.add(mutableListOf()); width = 0.0; pending = mutableListOf() }
                    isSpace -> if (tokenLines.last().isNotEmpty()) pending.add(token)
                    else -> {
                        val spaces = pending.sumOf { it.width }
                        if (tokenLines.last().isNotEmpty() && width + spaces + token.width > inlineSize) {
                            tokenLines.add(mutableListOf(token))
                            width = token.width
                        } else {
                            tokenLines.last().addAll(pending)
                            tokenLines.last().add(token)
                            width += spaces + token.width
                        }
                        pending = mutableListOf()
                    }
                }
            }
            for ((index, tokenLine) in tokenLines.withIndex()) {
                // consecutive tokens of the same run are placed as one segment
                val segments = mutableListOf<Segment>()
                var i = 0
                while (i < tokenLine.size) {
                    val run = tokenLine[i].run
                    val text = StringBuilder()
                    while (i < tokenLine.size && tokenLine[i].run === run) text.append(tokenLine[i++].text)
                    segments.add(Segment(run, text.toString()))
                }
                lines.add(Line(Vector2(x, y), index, segments))
            }
        } else {
            // parts of the text start at runs with a position of their own
            var position = Vector2(x, y)
            var segments = mutableListOf<Segment>()
            for (run in runs) {
                if (run !== first && (run.x != null || run.y != null)) {
                    lines.add(Line(position, 0, segments))
                    position = Vector2((run.x ?: position.x) + run.dx, (run.y ?: position.y) + run.dy)
                    segments = mutableListOf()
                }
                segments.add(Segment(run, run.text))
            }
            lines.add(Line(position, 0, segments))
        }

        val height = lineHeight(groupStyle, face)
        val arranged = mutableListOf<TextNode>()
        fun place(segment: Segment, px: Double?, py: Double?, rtlRun: Boolean) {
            arranged.add(TextNode(segment.text, null, px, py, rotate = segment.run.rotate).also {
                it.parent = segment.run.parent
                // a run placed at the left of its segment: anchored at its start, or at its end for right to left
                it.style = Style().also { s ->
                    s.properties.putAll(segment.run.style.properties)
                    s.textAnchor = if (rtlRun) TextAnchor.END else TextAnchor.START
                }
            })
        }
        for (line in lines) {
            if (line.segments.isEmpty()) continue
            val lineWidth = line.segments.sumOf { it.width }
            if (vertical) {
                // columns follow each other to the left, or to the right
                val columnX = line.position.x + line.index * height * (if (groupStyle.writingMode == WritingMode.VERTICAL_LR) 1.0 else -1.0)
                val top = line.position.y - lineWidth * anchor
                line.segments.forEachIndexed { i, segment ->
                    place(segment, if (i == 0) columnX else null, if (i == 0) top else null, false)
                }
                continue
            }
            val lineY = line.position.y + line.index * height
            // the left end of the line: right to left, the start of the line is on the right
            var left = if (rtl) line.position.x - lineWidth * (1.0 - anchor) else line.position.x - lineWidth * anchor
            val ordered = if (!bidi) line.segments else {
                // the segments ordered by where their characters are displayed
                val texts = line.segments.map { bidiText(it.run, it.text) }
                val starts = texts.runningFold(0) { acc, t -> acc + t.length }
                val analysis = BidiTextDriver.instance.analyze(texts.joinToString(""), if (rtl) Direction.RIGHT_TO_LEFT else Direction.LEFT_TO_RIGHT)
                val visual = analysis.visualOrder(0, starts.last())
                val positionOf = IntArray(visual.size).also { p -> visual.forEachIndexed { v, logical -> p[logical] = v } }
                line.segments.indices.sortedBy { i ->
                    (starts[i] until starts[i + 1]).minOfOrNull { positionOf[it] } ?: Int.MAX_VALUE
                }.map { line.segments[it] }
            }
            for (segment in ordered) {
                val segmentRtl = segment.run.effectiveStyle.direction == TextDirection.RTL
                place(segment, left, lineY, segmentRtl)
                left += segment.width
            }
        }
        return arranged
    }

    /**
     * The runs of [group] when it holds only text, in order, null otherwise
     */
    fun textRuns(group: GroupNode): List<TextNode>? = group.children.flatMap {
        when (it) {
            is TextNode -> listOf(it)
            is GroupNode -> textRuns(it) ?: return null
            else -> return null
        }
    }

    /** true when the fill or stroke of a run of [runs] is a paint that is drawn with a paint style */
    fun hasPaints(runs: List<TextNode>): Boolean = runs.any { servers(it.effectiveStyle) != null }

    /**
     * The text runs of [group] when they are all placed along the same contour, like the runs of an SVG textPath
     * element, null otherwise
     */
    fun contourRuns(group: GroupNode): List<TextNode>? {
        val runs = mutableListOf<TextNode>()
        fun collect(node: CompositionNode): Boolean = when (node) {
            is TextNode -> node.contour != null && (runs.isEmpty() || node.contour === runs.first().contour)
                .also { runs.add(node) }
            is GroupNode -> node.children.all(::collect)
            else -> false
        }
        return if (collect(group) && runs.isNotEmpty()) runs else null
    }

    /**
     * The glyphs of a run, placed, and the position after the run. [origin] is the start of the baseline of the run,
     * [rotate] the angle every glyph is turned by.
     */
    private class RunLayout(
        val glyphs: List<TypesetGlyph>,
        val end: Vector2,
        val origin: Vector2,
        val rotate: Double,
        /** the transforms of the outlines of glyphs that are not placed by their position, like those of vertical text */
        val outlines: Map<TypesetGlyph, Matrix44>? = null,
        /** the advance of the run along its line, from [origin] to the right or down */
        val advance: Double = (end - origin).length
    ) {
        /** true when glyphs are not placed by their positions alone */
        val transformed: Boolean get() = rotate != 0.0 || outlines != null

        /** the transform of the outline of [glyph], turned around its origin */
        fun outline(glyph: TypesetGlyph): Matrix44 = outlines?.get(glyph) ?: if (rotate == 0.0) glyph.transform() else
            buildTransform { translate(glyph.position); rotate(rotate); translate(-glyph.position) } * glyph.transform()
    }

    /**
     * [text] of [node] with the control characters of the unicode-bidi of the element it is in, a tspan or a text
     * element, in the direction of that element
     */
    private fun bidiText(node: TextNode, text: String): String {
        val element = node.parent ?: return text
        val rtl = element.effectiveStyle.direction == TextDirection.RTL
        return when (element.style.unicodeBidi) {
            UnicodeBidi.NORMAL -> text
            UnicodeBidi.EMBED -> (if (rtl) "\u202B" else "\u202A") + text + "\u202C"
            UnicodeBidi.ISOLATE -> (if (rtl) "\u2067" else "\u2066") + text + "\u2069"
            UnicodeBidi.BIDI_OVERRIDE -> (if (rtl) "\u202E" else "\u202D") + text + "\u202C"
            UnicodeBidi.ISOLATE_OVERRIDE -> (if (rtl) "\u2067\u202E" else "\u2066\u202D") + text + "\u202C\u2069"
            UnicodeBidi.PLAINTEXT -> "\u2068" + text + "\u2069"
        }
    }

    /** the script of upright vertical [text], for the vertical forms of fonts: kana, hangul or Han */
    private fun script(text: String): Script = when {
        text.any { it.code in 0x3040..0x309F } -> Script.HIRAGANA
        text.any { it.code in 0x30A0..0x30FF || it.code in 0xFF66..0xFF9F } -> Script.KATAKANA
        text.any { it.code in 0x1100..0x11FF || it.code in 0x3130..0x318F || it.code in 0xAC00..0xD7AF } -> Script.HANGUL
        else -> Script.HAN
    }

    /**
     * True when the character [c] is set upright in vertical text with text-orientation mixed: the characters of
     * scripts that are written vertically, like CJK ideographs, kana, hangul, their punctuation and full width forms,
     * following the Vertical_Orientation property of Unicode
     */
    private fun isUpright(c: Int): Boolean = c in 0x1100..0x11FF || c in 0x2E80..0x2FDF || c in 0x2FF0..0x31FF ||
            c in 0x3200..0x9FFF || c in 0xA000..0xA4CF || c in 0xA960..0xA97F || c in 0xAC00..0xD7FF ||
            c in 0xF900..0xFAFF || c in 0xFE10..0xFE1F || c in 0xFE30..0xFE4F || c in 0xFF00..0xFF60 ||
            c in 0xFFE0..0xFFE6 || c in 0x1F000..0x1FAFF || c in 0x20000..0x3FFFD

    /**
     * Lays out the text of [node] vertically, from top to bottom: upright characters one below the other, centered
     * on the line, and runs of other characters turned 90 degrees clockwise, centered on the line by the middle of
     * their ascent and descent, like the central baseline of vertical text. The anchor is along the line.
     */
    private fun layoutVerticalRun(node: TextNode, pen: Vector2, adjust: Boolean): RunLayout? {
        val style = node.effectiveStyle
        val face = face(style, node) ?: return null
        val textStyle = textStyle(node, style, adjust)
        val fontSize = fontSize(style)
        val letterSpacing = pixels(style.letterSpacing, fontSize)
        val ascent = abs(face.ascent)
        val descent = abs(face.descent)
        val orientation = style.textOrientation

        // the text in segments of upright and sideways characters
        val segments = mutableListOf<Pair<Boolean, String>>()
        var offset = 0
        while (offset < node.text.length) {
            val codePoint = if (node.text[offset].isHighSurrogate() && offset + 1 < node.text.length)
                (node.text[offset].code - 0xD800) * 0x400 + (node.text[offset + 1].code - 0xDC00) + 0x10000 else node.text[offset].code
            val length = if (codePoint > 0xFFFF) 2 else 1
            val upright = when (orientation) {
                TextOrientation.UPRIGHT -> true
                TextOrientation.SIDEWAYS -> false
                TextOrientation.MIXED -> isUpright(codePoint)
            }
            val character = node.text.substring(offset, offset + length)
            if (segments.isNotEmpty() && segments.last().first == upright && !upright) {
                segments[segments.size - 1] = Pair(false, segments.last().second + character)
            } else {
                segments.add(Pair(upright, character))
            }
            offset += length
        }

        // the glyphs relative to the start of the line, and the transforms of their outlines
        val placed = mutableListOf<Pair<TypesetGlyph, Matrix44>>()
        var cursor = 0.0
        val space = face.glyphForCharacter(' ').advanceWidth() + letterSpacing + pixels(style.wordSpacing, fontSize)
        for ((upright, text) in segments) {
            if (upright) {
                // shaped vertically: the vertical forms of the font, its vertical advances, the glyphs centered on
                // the line by their offsets
                val glyphStyle = textStyle.copy(face = face)
                val shaped = TextShapingDriver.instance.shape(face, text, textStyle.features, Direction.TOP_TO_BOTTOM, script(text))
                for (result in shaped) {
                    val position = Vector2(0.0, cursor) + result.offset
                    val g = TypesetGlyph(face, result.glyphIndex, position, glyphStyle)
                    placed.add(g to g.transform())
                    cursor += result.advance.y + letterSpacing
                }
            } else {
                val leading = text.takeWhile { it == ' ' }.length * space
                val trailing = if (text.isBlank()) 0.0 else text.takeLastWhile { it == ' ' }.length * space
                val glyphs = typesetLine(face, text.trim(' '), Vector2(leading, 0.0), textStyle)
                val width = (glyphs.maxOfOrNull { it.penEnd?.x ?: (it.position.x + face.glyphForIndex(it.glyphIndex).advanceWidth() + letterSpacing) } ?: leading) + trailing
                // turned clockwise around the start of the segment, the middle of the ascent and descent on the line
                val turn = buildTransform {
                    translate(-(ascent - descent) / 2.0, cursor)
                    rotate(90.0)
                }
                for (glyph in glyphs) placed.add(glyph to turn * glyph.transform())
                cursor += width
            }
        }

        val start = Vector2(node.x ?: pen.x, node.y ?: pen.y) + Vector2(node.dx, node.dy)
        val origin = start - Vector2(0.0, cursor * style.textAnchor.value)
        val toOrigin = buildTransform { translate(origin) }
        val outlines = HashMap<TypesetGlyph, Matrix44>()
        val glyphs = placed.map { (glyph, transform) ->
            outlines[glyph] = toOrigin * transform
            glyph
        }
        return RunLayout(glyphs, origin + Vector2(0.0, cursor), origin, 0.0, outlines, cursor)
    }

    /**
     * Lays out the text of [node] starting at [pen], or at the position of the node when it has one
     */
    private fun layoutRun(node: TextNode, pen: Vector2, adjust: Boolean = true): RunLayout? {
        if (node.effectiveStyle.writingMode.vertical) return layoutVerticalRun(node, pen, adjust)
        val style = node.effectiveStyle
        val face = face(style, node) ?: return null
        val adjustment = if (adjust) adjustment(node) else noAdjustment
        val rtl = style.direction == TextDirection.RTL
        // right to left text is shaped as a paragraph in that direction, embedded or overridden by unicode-bidi
        val textStyle = textStyle(node, style, adjust).copy(direction = if (rtl) Direction.RIGHT_TO_LEFT else Direction.LEFT_TO_RIGHT)
        val text = bidiText(node, node.text.trim(' '))

        // spaces at the start and the end of a run separate it from the runs around it, the typesetter does not keep
        // them, their advances are added here; in right to left text the start is on the right
        val fontSize = fontSize(style)
        val letterSpacing = pixels(style.letterSpacing, fontSize) + adjustment.spacing
        val factor = adjustment.factor
        val space = (face.glyphForCharacter(' ').advanceWidth() + letterSpacing + pixels(style.wordSpacing, fontSize)) * factor
        val logicalLeading = node.text.takeWhile { it == ' ' }.length * space
        val logicalTrailing = if (node.text.isBlank()) 0.0 else node.text.takeLastWhile { it == ' ' }.length * space
        val leading = if (rtl) logicalTrailing else logicalLeading
        val trailing = if (rtl) logicalLeading else logicalTrailing
        val glyphs = typesetLine(face, text, Vector2(leading, 0.0), textStyle)
        // where the pen is after a glyph: its shaped advance includes the spacing, invisible glyphs, like the controls
        // of bidirectional text, do not advance
        fun glyphEnd(glyph: TypesetGlyph) = glyph.penEnd?.x
            ?: (glyph.position.x + (face.glyphForIndex(glyph.glyphIndex).advanceWidth() + letterSpacing) * factor)
        // the advance of the text, used for its anchor and to continue the text after it; the spacing that adjusts
        // text to its length is not added after its last character
        val width = (glyphs.maxOfOrNull { glyphEnd(it) } ?: leading) + trailing -
                (if (adjustment.last === node) adjustment.spacing else 0.0)

        val start = Vector2(node.x ?: pen.x, node.y ?: pen.y) + Vector2(node.dx, node.dy)
        // right to left, the start of text is on the right: text that continues ends where the text before it starts
        val origin = when {
            !rtl -> start - Vector2(width * style.textAnchor.value, 0.0)
            node.x != null -> start - Vector2(width * (1.0 - style.textAnchor.value), 0.0)
            else -> start - Vector2(width, 0.0)
        }
        // the glyphs are on the alphabetic baseline, text continues on the baseline of the position
        val baseline = Vector2(0.0, baselineOffset(node, style, face))
        return RunLayout(
            glyphs.map { TypesetGlyph(it.face, it.glyphIndex, it.position + origin + baseline, it.style, it.advance, it.offset) },
            if (rtl) origin else origin + Vector2(width, 0.0),
            origin + baseline,
            node.rotate,
            advance = width
        )
    }

    /**
     * Draws the text of [node] starting at [pen], or at the position of the node when it has one. Text with a
     * contour is drawn along it, the x component of the pen and of the position is then the distance along the
     * contour. Paints over the bounds of the text are mapped to [textBounds], the bounds of the text element.
     * @return the position after the text, where text that continues it starts
     */
    fun draw(drawer: Drawer, node: TextNode, pen: Vector2, textBounds: Rectangle? = null): Vector2 {
        if (node.contour != null) {
            return drawOnContour(drawer, listOf(node), pen, textBounds)
        }
        val layout = layoutRun(node, pen) ?: return pen
        val style = node.effectiveStyle
        val visible = layout.glyphs.filter { it.style.fill != null || it.style.stroke != null }
        // a run that is not part of a text element is a text of its own
        val bounds = textBounds ?: layout.glyphs.flatMap { cell(it, layout.outline(it)) }.takeIf { it.isNotEmpty() }?.bounds
        // hidden text is laid out, text after it continues after it, but it is not drawn
        if (visible.isNotEmpty() && style.visibility.value && applyPaints(drawer, style, bounds)) {
            slugTextDrawer.clear()
            if (!layout.transformed) slugTextDrawer.addGlyphs(visible)
            else slugTextDrawer.addGlyphs(visible) { layout.outline(it) }
            drawer.applyClip()
            slugTextDrawer.draw(drawer)
            drawDecorations(drawer, node, style, layout)
        }
        return layout.end
    }

    /**
     * Draws the lines of the text decoration of [style] along the run of [node] laid out as [layout], in the fill
     * and stroke of the text
     */
    private fun drawDecorations(drawer: Drawer, node: TextNode, style: Style, layout: RunLayout) {
        val decoration = style.textDecoration
        if (!decoration.underline && !decoration.overline && !decoration.lineThrough) return
        val face = face(style, node) ?: return
        val textStyle = textStyle(node, style)
        // the underline and the line through are where the face puts them, the overline is on top of the ascent
        val metrics = face.decorationMetrics
        val ascent = abs(face.ascent)
        val lines = listOfNotNull(
            Pair(-metrics.underlinePosition, metrics.underlineThickness).takeIf { decoration.underline },
            Pair(-ascent - metrics.underlineThickness / 2.0, metrics.underlineThickness).takeIf { decoration.overline },
            Pair(-metrics.strikeoutPosition, metrics.strikeoutThickness).takeIf { decoration.lineThrough }
        )
        if (layout.advance <= 0.0) return
        drawer.pushStyle()
        drawer.fill = textStyle.fill
        drawer.stroke = textStyle.stroke
        drawer.strokeWeight = textStyle.strokeWeight ?: 1.0
        val rectangles = if (style.writingMode.vertical) {
            // the lines of vertical text turn with its glyphs: the underline is on the left of the line, the overline
            // on the right; the line is the central baseline, the middle of the ascent and the descent
            val central = (ascent - abs(face.descent)) / 2.0
            lines.map { (y, thickness) ->
                Rectangle(layout.origin.x - central - y - thickness / 2.0, layout.origin.y, thickness, layout.advance)
            }
        } else {
            lines.map { (y, thickness) ->
                Rectangle(layout.origin.x, layout.origin.y + y - thickness / 2.0, layout.advance, thickness)
            }
        }
        drawer.applyClip()
        drawer.rectangles(rectangles)
        drawer.popStyle()
    }

    /**
     * Lays out [runs] that share a contour along it, like SVG places the text of a textPath element. Runs that are
     * not [shown] are laid out without fill and stroke.
     */
    private fun layoutOnContour(
        runs: List<TextNode>,
        pen: Vector2,
        shown: (TextNode) -> Boolean = { true }
    ): ContourTextLayout? {
        val first = runs.first()
        val contour = first.contour ?: return null
        val spans = mutableListOf<TextSpan>()
        val shifts = mutableListOf<Vector2>()
        // shifts accumulate, the baseline of every run is shifted by the difference with the previous run
        var previousBaseline = 0.0
        for (run in runs) {
            val style = run.effectiveStyle
            val face = face(style, run) ?: continue
            val textStyle = textStyle(run, style).copy(face = face)
            spans.add(TextSpan(run.text, if (shown(run)) textStyle else textStyle.copy(fill = null, stroke = null)))
            val baseline = baselineOffset(run, style, face)
            shifts.add(Vector2(run.dx, run.dy + baseline - previousBaseline))
            previousBaseline = baseline
        }
        if (spans.isEmpty()) {
            return null
        }
        return layoutTextOnContour(
            spans,
            ContourBaseline(contour, measureOnBaseline = false),
            // the fill and stroke of every span are complete, a span without a fill is not filled
            style = TextStyle.Defaults.copy(fill = null),
            startDistance = first.x ?: pen.x,
            anchor = first.effectiveStyle.textAnchor.value,
            overflow = ContourOverflow.HIDE,
            pivot = GlyphPivot.MIDDLE,
            shifts = shifts
        )
    }

    /**
     * Draws [runs] that share a contour along it, like SVG draws the text of a textPath element: the runs are shaped
     * as a single line, the middle of every glyph is placed on the contour and glyphs beyond its ends are not drawn.
     * The first run determines where the text starts and its anchor. The dx of every run moves the text from that run
     * on along the contour, its dy moves it across the contour. Runs with different paints are drawn separately,
     * paints over the bounds of the text are mapped to [textBounds].
     * @return the position after the text, its x component is the distance along the contour
     */
    fun drawOnContour(drawer: Drawer, runs: List<TextNode>, pen: Vector2, textBounds: Rectangle? = null): Vector2 {
        val all = layoutOnContour(runs, pen) ?: return pen
        // runs that are not part of a text element are a text of their own
        val bounds = textBounds ?: all.glyphs.flatMap { cell(it.glyph, it.transform) }.takeIf { it.isNotEmpty() }?.bounds
        // the runs are drawn in groups that share their paints
        val groups = runs.groupBy { servers(it.effectiveStyle) }
        fun shown(run: TextNode) = run.effectiveStyle.visibility.value
        val anyHidden = runs.any { !shown(it) }
        for ((paints, groupRuns) in groups) {
            val layout = if (groups.size == 1 && !anyHidden) all else
                layoutOnContour(runs, pen) { it in groupRuns && shown(it) } ?: continue
            val visible = layout.glyphs.filter { it.glyph.style.fill != null || it.glyph.style.stroke != null }
            if (visible.isEmpty()) continue
            drawer.pushStyle()
            val drawn = paints == null || applyPaints(drawer, groupRuns.first().effectiveStyle, bounds)
            if (drawn) {
                slugTextDrawer.clear()
                slugTextDrawer.addGlyphs(ContourTextLayout(visible, layout.start, layout.advance))
                drawer.applyClip()
                slugTextDrawer.draw(drawer)
            }
            drawer.popStyle()
        }
        return Vector2(all.end, pen.y)
    }

    /**
     * The corners of the glyph cell of [glyph], its advance by the ascent and descent of its face, in the coordinates
     * of the text. [outline] is the transform of the outline of the glyph when it is not placed by its position, as
     * for text along a contour.
     */
    private fun cell(glyph: TypesetGlyph, outline: Matrix44? = null): List<Vector2> {
        val face = glyph.face
        val advance = face.glyphForIndex(glyph.glyphIndex).advanceWidth()
        val p = glyph.position
        val top = p.y - abs(face.ascent)
        val bottom = p.y + abs(face.descent)
        val corners = listOf(Vector2(p.x, top), Vector2(p.x + advance, top), Vector2(p.x + advance, bottom), Vector2(p.x, bottom))
        if (outline == null) return corners
        val m = outline * glyph.transform().inversed
        return corners.map { (m * it.xy01).xy }
    }

    /**
     * The bounds of the text of [group], the union of the cells of its glyphs, like SVG's bounding box of a text
     * element, laid out starting at [pen]. Null when it has no glyphs.
     */
    fun measure(group: GroupNode, pen: Vector2): Rectangle? {
        val points = mutableListOf<Vector2>()
        var current = pen
        fun visit(node: CompositionNode) {
            when (node) {
                is TextNode -> if (node.contour != null) {
                    layoutOnContour(listOf(node), current)?.let { layout ->
                        layout.glyphs.forEach { points.addAll(cell(it.glyph, it.transform)) }
                        current = Vector2(layout.end, current.y)
                    }
                } else {
                    layoutRun(node, current)?.let { layout ->
                        layout.glyphs.forEach { points.addAll(if (!layout.transformed) cell(it) else cell(it, layout.outline(it))) }
                        current = layout.end
                    }
                }

                is GroupNode -> {
                    val wrapped = if (node.textElement) arrange(node) else null
                    if (wrapped != null) {
                        for (run in wrapped) layoutRun(run, current)?.let { layout ->
                            layout.glyphs.forEach { points.addAll(cell(it)) }
                            current = layout.end
                        }
                        return
                    }
                    val contourRuns = contourRuns(node)
                    if (contourRuns != null) {
                        layoutOnContour(contourRuns, current)?.let { layout ->
                            layout.glyphs.forEach { points.addAll(cell(it.glyph, it.transform)) }
                            current = Vector2(layout.end, current.y)
                        }
                    } else {
                        node.children.forEach(::visit)
                    }
                }

                else -> {}
            }
        }
        visit(group)
        return if (points.isEmpty()) null else points.bounds
    }
}
