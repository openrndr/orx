package org.openrndr.extra.textwriter

import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.contains
import kotlin.jvm.JvmName

/**
 * A glyph placed by [typesetText].
 * @param position the position of the glyph's origin on the baseline, includes the baseline shift
 * @param style the fully cascaded style of the glyph
 */
class TypesetGlyph(val face: Face, val glyphIndex: Int, val position: Vector2, val style: TextStyle) {
    /**
     * The transform that places the outline of the glyph, as returned by [org.openrndr.draw.font.Glyph.shape].
     */
    fun transform(): Matrix44 {
        val sizeInEm = style.sizeInEm ?: error("sizeInEm not set")
        val textWidthFactor = style.textWidthFactor ?: error("textWidthFactor not set")
        return transform {
            translate(position)
            if (sizeInEm != 1.0 || textWidthFactor != 1.0) {
                scale(sizeInEm * textWidthFactor, sizeInEm, 1.0)
            }
        }
    }
}

/**
 * A line of glyphs placed by [typesetText].
 * @param boxIndex the index of the box or shape the line is placed in
 */
class TypesetLine(val boxIndex: Int, val glyphs: List<TypesetGlyph>)

/**
 * Typesets [spans] into [boxes] using Knuth-Plass line breaking. Text flows from one box to the next,
 * lines that do not fit in any of the boxes are placed in the last box.
 *
 * Typesetting only computes glyph positions, it does not draw. Use [SlugTextDrawer] to draw text or
 * [VectorTextDrawer] to produce glyph outlines.
 *
 * @param style the style that is cascaded into the style of every span, layout properties
 * (line height, alignment, justification, direction) are taken from the style of the first span
 * @return the typeset lines, in order
 */
fun typesetText(
    spans: List<TextSpan>,
    boxes: List<Rectangle>,
    style: TextStyle = TextStyle.Defaults,
    shaper: TextShapingDriver = TextShapingDriver.instance
): List<TypesetLine> {
    require(boxes.isNotEmpty()) { "boxes must not be empty" }

    if (spans.isEmpty())
        return emptyList()

    // Resolve the base style from the first span (used for layout-level properties)
    val baseStyle = style.cascade(spans.first().style)

    baseStyle.lineHeightInEm ?: error("lineHeightInEm not set")
    baseStyle.verticalAlign ?: error("verticalAlign not set")
    baseStyle.horizontalAlign ?: error("horizontalAlign not set")
    baseStyle.justify ?: error("justify not set")

    val baseFace = baseStyle.face ?: error("face not set")
    val lineHeight = (baseFace.ascent - baseFace.descent + baseFace.lineGap) * baseStyle.lineHeightInEm

    // Build items list from all spans (Box, Glue, Penalty per Knuth-Plass)
    val paragraph = shapeParagraph(spans, style, baseStyle.direction, shaper = shaper)
    val items = paragraph.items

    // Compute the maximum number of lines that fit in each box
    val boxLineCapacities = boxes.map { box ->
        val maxLines = ((box.height - baseFace.ascent + baseFace.descent) / lineHeight + 1).toInt()
        maxOf(maxLines, 1)
    }

    // Each line uses the width of the box it belongs to, lines that don't fit use the first box's width
    val lineWidths = boxes.indices.flatMap { boxIdx -> List(boxLineCapacities[boxIdx]) { boxes[boxIdx].width } }
    val defaultLineWidth = boxes.first().width

    val lines = breakLines(items) { lineIndex ->
        if (lineIndex < lineWidths.size) lineWidths[lineIndex] else defaultLineWidth
    }

    // Pre-compute which box each line belongs to
    val totalLines = lines.size
    val lineBoxIndex = IntArray(totalLines)
    val lineIndexInBox = IntArray(totalLines)
    run {
        var bi = 0
        var lic = 0
        for (li in 0 until totalLines) {
            while (bi < boxes.size && lic >= boxLineCapacities[bi]) {
                bi++
                lic = 0
            }
            if (bi >= boxes.size) {
                // Lines that don't fit are assigned to the last box (will be clipped)
                lineBoxIndex[li] = boxes.size - 1
                lineIndexInBox[li] = lic
            } else {
                lineBoxIndex[li] = bi
                lineIndexInBox[li] = lic
            }
            lic++
        }
    }

    // Count total lines per box
    val linesPerBox = IntArray(boxes.size)
    for (li in 0 until totalLines) {
        linesPerBox[lineBoxIndex[li]]++
    }

    return lines.mapIndexed { lineIdx, line ->
        val currentBoxIndex = lineBoxIndex[lineIdx]
        val box = boxes[currentBoxIndex]
        val totalLinesInBox = linesPerBox[currentBoxIndex]

        val totalTextHeightInBox = baseFace.ascent + (totalLinesInBox - 1).coerceAtLeast(0) * lineHeight - baseFace.descent
        val verticalOffset = (box.height - totalTextHeightInBox) * baseStyle.verticalAlign
        val y = box.y + baseFace.ascent + verticalOffset + lineIndexInBox[lineIdx] * lineHeight

        placeLine(paragraph, line, baseStyle, box.x, box.width, y, currentBoxIndex)
    }
}

/**
 * Typesets [spans] into [shapes] using Knuth-Plass line breaking. Every shape is filled from top to bottom with
 * lines spaced by the line height, a line is placed where the full height of the line (from ascent to descent of
 * the face of the first span) fits inside the shape. Where a shape is interrupted, by a hole or a concave part, a
 * single baseline can hold multiple lines, these are filled from left to right, or from right to left for
 * right-to-left paragraphs. Parts narrower than the line height are skipped. Text flows from one shape to the next,
 * text that does not fit in any of the shapes is discarded.
 *
 * Vertical alignment moves the text in every shape down by the vertical alignment times the largest distance it can
 * be moved down without the shape holding less text. For a rectangle this is the same as the vertical alignment in
 * a box. A shape that is filled and passes text on to the next shape can usually move less than a line.
 *
 * Typesetting only computes glyph positions, it does not draw. Use [SlugTextDrawer] to draw text or
 * [VectorTextDrawer] to produce glyph outlines.
 *
 * @param style the style that is cascaded into the style of every span, layout properties
 * (line height, alignment, justification, direction) are taken from the style of the first span
 * @return the typeset lines, in order, [TypesetLine.boxIndex] is the index of the shape
 */
@JvmName("typesetTextInShapes")
fun typesetText(
    spans: List<TextSpan>,
    shapes: List<Shape>,
    style: TextStyle = TextStyle.Defaults,
    shaper: TextShapingDriver = TextShapingDriver.instance
): List<TypesetLine> {
    require(shapes.isNotEmpty()) { "shapes must not be empty" }

    if (spans.isEmpty())
        return emptyList()

    val baseStyle = style.cascade(spans.first().style)
    baseStyle.lineHeightInEm ?: error("lineHeightInEm not set")
    baseStyle.horizontalAlign ?: error("horizontalAlign not set")
    val verticalAlign = baseStyle.verticalAlign ?: error("verticalAlign not set")
    baseStyle.justify ?: error("justify not set")

    val baseFace = baseStyle.face ?: error("face not set")
    val lineHeight = (baseFace.ascent - baseFace.descent + baseFace.lineGap) * baseStyle.lineHeightInEm

    val paragraph = shapeParagraph(spans, style, baseStyle.direction, shaper = shaper)
    val rightToLeft = (paragraph.bidi.paragraphs.firstOrNull()?.level ?: 0) % 2 == 1

    fun layout(offsets: DoubleArray): Pair<List<LineSlot>, List<BrokenLine>> {
        val slots = lineSlots(shapes, offsets, baseFace.ascent, baseFace.descent, lineHeight, rightToLeft)
        if (slots.isEmpty()) return slots to emptyList()
        return slots to breakLines(paragraph.items) { lineIndex -> slots[minOf(lineIndex, slots.lastIndex)].width }
    }

    val offsets = DoubleArray(shapes.size)
    if (verticalAlign != 0.0) {
        alignVertically(shapes, offsets, verticalAlign, lineHeight, ::layout)
    }

    val (slots, lines) = layout(offsets)
    if (slots.isEmpty())
        return emptyList()

    // lines that do not fit in any of the shapes are discarded
    return lines.take(slots.size).mapIndexed { lineIdx, line ->
        val slot = slots[lineIdx]
        placeLine(paragraph, line, baseStyle, slot.x, slot.width, slot.baseline, slot.shapeIndex)
    }
}

/**
 * Sets the vertical offset of every shape in [offsets], in order: a shape is moved down by [verticalAlign] times the
 * largest offset at which the text placed in it and the shapes before it does not decrease. Offsets are searched
 * in steps of a quarter line and refined by bisection.
 */
private fun alignVertically(
    shapes: List<Shape>,
    offsets: DoubleArray,
    verticalAlign: Double,
    lineHeight: Double,
    layout: (DoubleArray) -> Pair<List<LineSlot>, List<BrokenLine>>
) {
    // the number of items placed in the shapes up to and including shapeIndex
    fun placed(shapeIndex: Int): Int {
        val (slots, lines) = layout(offsets)
        var end = 0
        for ((lineIndex, line) in lines.take(slots.size).withIndex()) {
            if (slots[lineIndex].shapeIndex <= shapeIndex) end = line.end
        }
        return end
    }

    for (shapeIndex in shapes.indices) {
        if (shapes[shapeIndex].empty) continue
        offsets[shapeIndex] = 0.0
        val required = placed(shapeIndex)
        if (required == 0) break

        val step = lineHeight / 4.0
        var best = 0.0
        var candidate = step
        while (candidate <= shapes[shapeIndex].bounds.height) {
            offsets[shapeIndex] = candidate
            if (placed(shapeIndex) >= required) best = candidate
            candidate += step
        }

        // refine between the best offset and the next step, which does not fit or is outside the shape
        var low = best
        var high = best + step
        repeat(12) {
            val middle = (low + high) / 2.0
            offsets[shapeIndex] = middle
            if (placed(shapeIndex) >= required) low = middle else high = middle
        }
        offsets[shapeIndex] = low * verticalAlign
    }
}

/**
 * A horizontal stretch inside a shape that holds a single line.
 */
private class LineSlot(val shapeIndex: Int, val x: Double, val width: Double, val baseline: Double)

/**
 * Finds the line slots in [shapes]: for every baseline, spaced [lineHeight] apart from the top of a shape moved down
 * by the shape's offset in [offsets], the stretches in which the line from [ascent] above to [descent] below the
 * baseline is inside the shape.
 */
private fun lineSlots(
    shapes: List<Shape>,
    offsets: DoubleArray,
    ascent: Double,
    descent: Double,
    lineHeight: Double,
    rightToLeft: Boolean
): List<LineSlot> {
    val slots = mutableListOf<LineSlot>()
    val minimumWidth = lineHeight
    // the band of a line is sampled at a number of heights, samples are kept slightly away from the band edges
    val samples = 5
    val epsilon = lineHeight * 1E-3

    for ((shapeIndex, shape) in shapes.withIndex()) {
        if (shape.empty) continue
        val polylines = shape.contours.map { it.adaptivePositions() }
        val bounds = shape.bounds

        var baseline = bounds.y + offsets[shapeIndex] + ascent
        while (baseline - descent <= bounds.y + bounds.height + 1E-6) {
            val top = baseline - ascent + epsilon
            val bottom = baseline - descent - epsilon
            var intervals = insideIntervals(shape, polylines, top)
            for (sample in 1 until samples) {
                if (intervals.isEmpty()) break
                val y = top + (bottom - top) * sample / (samples - 1)
                intervals = intersectIntervals(intervals, insideIntervals(shape, polylines, y))
            }
            val lineIntervals = intervals.filter { it.second - it.first >= minimumWidth }
            for (interval in if (rightToLeft) lineIntervals.asReversed() else lineIntervals) {
                slots.add(LineSlot(shapeIndex, interval.first, interval.second - interval.first, baseline))
            }
            baseline += lineHeight
        }
    }
    return slots
}

/**
 * The horizontal intervals at height [y] that are inside [shape], sorted from left to right.
 */
private fun insideIntervals(shape: Shape, polylines: List<List<Vector2>>, y: Double): List<Pair<Double, Double>> {
    val crossings = mutableListOf<Double>()
    for (points in polylines) {
        for (i in points.indices) {
            val p = points[i]
            val q = points[(i + 1) % points.size]
            if ((p.y > y) != (q.y > y)) {
                crossings.add(p.x + (y - p.y) * (q.x - p.x) / (q.y - p.y))
            }
        }
    }
    crossings.sort()

    val intervals = mutableListOf<Pair<Double, Double>>()
    for (i in 0 until crossings.size - 1) {
        val left = crossings[i]
        val right = crossings[i + 1]
        if (right - left <= 0.0 || Vector2((left + right) / 2.0, y) !in shape) continue
        val last = intervals.lastOrNull()
        if (last != null && last.second >= left) {
            intervals[intervals.lastIndex] = last.first to right
        } else {
            intervals.add(left to right)
        }
    }
    return intervals
}

private fun intersectIntervals(a: List<Pair<Double, Double>>, b: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
    val result = mutableListOf<Pair<Double, Double>>()
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) {
        val left = maxOf(a[i].first, b[j].first)
        val right = minOf(a[i].second, b[j].second)
        if (left < right) result.add(left to right)
        if (a[i].second < b[j].second) i++ else j++
    }
    return result
}

/**
 * Places a [line] of [paragraph] at baseline [y], in the horizontal range from [x] with [width], aligned using the
 * horizontal alignment of [baseStyle].
 */
private fun placeLine(
    paragraph: ShapedParagraph,
    line: BrokenLine,
    baseStyle: TextStyle,
    x: Double,
    width: Double,
    y: Double,
    boxIndex: Int
): TypesetLine {
    val items = paragraph.items
    val justify = baseStyle.justify ?: error("justify not set")
    val horizontalAlign = baseStyle.horizontalAlign ?: error("horizontalAlign not set")

    fun glueWidth(glue: LineGlue) = glue.adjustedWidth(line.ratio, justify, line.isLast)

    // Compute natural line width for horizontal alignment
    var naturalLineWidth = 0.0
    for (idx in line.start until line.end) {
        when (val item = items[idx]) {
            is LineBox -> naturalLineWidth += item.width
            is LineGlue -> naturalLineWidth += glueWidth(item)
            is LinePenalty -> {}
        }
    }
    // A line that ends at a hyphenation point shows a hyphen after its last word
    val hyphenatedItem = if (line.hyphenated) {
        naturalLineWidth += items[line.end].width
        (line.end - 1 downTo line.start).firstOrNull { items[it] is LineBox } ?: -1
    } else -1
    val horizontalOffset = (width - naturalLineWidth) * horizontalAlign

    val glyphs = mutableListOf<TypesetGlyph>()
    placeItems(
        items, paragraph.visualOrder(line.start, line.end), Vector2(x + horizontalOffset, y), ::glueWidth, glyphs,
        hyphenatedItem
    )
    return TypesetLine(boxIndex, glyphs)
}

/**
 * Places the glyphs of [items] in the given visual [order] starting at [start], appends them to [glyphs].
 * Applies the size, width factor and baseline shift of the style of every word. The hyphen of the word at
 * [hyphenatedItem] is placed at the end of that word, which is on the left for right-to-left words.
 */
private fun placeItems(
    items: List<LineItem<ShapedWord<TextStyle>>>,
    order: IntArray,
    start: Vector2,
    glueWidth: (LineGlue) -> Double,
    glyphs: MutableList<TypesetGlyph>,
    hyphenatedItem: Int = -1
) {
    var cursor = start
    for (idx in order) {
        when (val item = items[idx]) {
            is LineBox -> {
                val wordStyle = item.content.style
                val wordFace = wordStyle.face ?: error("face not set")
                val sizeInEm = wordStyle.sizeInEm ?: error("sizeInEm not set")
                val scale = sizeInEm * (wordStyle.textWidthFactor ?: error("textWidthFactor not set"))
                val baselineShift = Vector2(0.0, -(wordStyle.baselineShiftInEm ?: 0.0) * wordFace.height * sizeInEm)
                fun place(shapeResults: List<ShapeResult>) {
                    for (sr in shapeResults) {
                        glyphs.add(TypesetGlyph(wordFace, sr.glyphIndex, cursor + sr.offset * scale + baselineShift, wordStyle))
                        cursor += sr.advance * scale
                    }
                }

                val hyphen = if (idx == hyphenatedItem) item.content.hyphen else emptyList()
                if (item.content.rightToLeft) {
                    place(hyphen)
                    place(item.content.shapeResults)
                } else {
                    place(item.content.shapeResults)
                    place(hyphen)
                }
            }

            is LineGlue -> cursor += Vector2(glueWidth(item), 0.0)
            is LinePenalty -> {}
        }
    }
}

/**
 * Places a single line of [text] in [face] starting at [position] on the baseline, without line breaking or
 * alignment. Like [typesetText] it applies the size, width factor, baseline shift and shaping features of
 * [style] and orders mixed-direction text using the paragraph direction of [style], newlines are treated as spaces.
 */
fun typesetLine(
    face: Face,
    text: String,
    position: Vector2,
    style: TextStyle = TextStyle.Defaults,
    shaper: TextShapingDriver = TextShapingDriver.instance
): List<TypesetGlyph> {
    // A single line has no line breaks, newlines are treated as spaces
    val paragraph = shapeParagraph(
        listOf(TextSpan(text.replace('\n', ' '), null)), style.copy(face = face), style.direction, hyphenate = false, shaper = shaper
    )

    val glyphs = mutableListOf<TypesetGlyph>()
    placeItems(paragraph.items, paragraph.visualOrder(0, paragraph.items.size), position, { it.width }, glyphs)
    return glyphs
}
