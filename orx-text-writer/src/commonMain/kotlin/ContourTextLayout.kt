package org.openrndr.extra.textwriter

import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.asDegrees
import org.openrndr.math.transforms.transform
import org.openrndr.shape.ShapeContour
import kotlin.math.atan2

/**
 * What happens to glyphs that are placed beyond the start or the end of a contour
 */
enum class ContourOverflow {
    /** glyphs continue at the start of a closed contour, on an open contour they are placed like [CLAMP] */
    WRAP,

    /** glyphs are placed at the start or the end of the contour */
    CLAMP,

    /** glyphs of which the pivot is not on the contour are not placed, like text on a path in SVG */
    HIDE
}

/**
 * The point of a glyph that is placed on the contour, the glyph is rotated around it
 */
enum class GlyphPivot {
    /** the start of the glyph on the baseline */
    START,

    /** the middle of the advance of the glyph on the baseline, like text on a path in SVG */
    MIDDLE
}

/**
 * The baseline for text on a [contour]: the curve at distance [offset] from the contour, positive distances move the
 * baseline towards the ascenders of the text. Distances along the baseline map to positions on the contour.
 *
 * @param measureOnBaseline measure distances on the offset curve, so glyph spacing is correct on the offset curve,
 * which is longer than the contour on the outside of a bend and shorter on the inside. When false distances are
 * measured on the contour, like text on a path in SVG.
 * @param samplesPerSegment the number of samples per segment used to measure the contour
 */
class ContourBaseline(
    val contour: ShapeContour,
    val offset: Double = 0.0,
    measureOnBaseline: Boolean = true,
    samplesPerSegment: Int = 64
) {
    private val sampleCount = (contour.segments.size * samplesPerSegment).coerceAtLeast(1)

    // the distance along the baseline of every sample, samples are taken at equal contour t steps
    private val distances = DoubleArray(sampleCount + 1).also { distances ->
        if (!contour.empty) {
            fun measured(t: Double) = if (measureOnBaseline) point(t) else contour.position(t)
            var previous = measured(0.0)
            for (i in 1..sampleCount) {
                val current = measured(i.toDouble() / sampleCount)
                distances[i] = distances[i - 1] + current.distanceTo(previous)
                previous = current
            }
        }
    }

    /** the length of the baseline */
    val length: Double get() = distances[sampleCount]

    val closed: Boolean get() = contour.closed

    // the direction of the ascenders of upright text is to the left of the direction of the contour
    private fun ascender(t: Double): Vector2 = contour.direction(t).let { Vector2(it.y, -it.x) }

    private fun point(t: Double): Vector2 =
        if (offset == 0.0) contour.position(t) else contour.position(t) + ascender(t) * offset

    /** the contour t of the point at [distance] along the baseline */
    fun t(distance: Double): Double {
        if (distance <= 0.0) return 0.0
        if (distance >= length) return 1.0
        var low = 0
        var high = sampleCount
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (distances[middle] < distance) low = middle else high = middle
        }
        val span = distances[high] - distances[low]
        val fraction = if (span > 0.0) (distance - distances[low]) / span else 0.0
        return (low + fraction) / sampleCount
    }

    /**
     * The frame at [distance] along the baseline: the x-axis follows the contour, the y-axis points away from the
     * ascenders, like the y-axis of the glyph positions of [typesetText].
     */
    fun pose(distance: Double): Matrix44 {
        val t = t(distance)
        val direction = contour.direction(t)
        return transform {
            translate(point(t))
            rotate(Vector3.UNIT_Z, atan2(direction.y, direction.x).asDegrees)
        }
    }
}

/**
 * A glyph placed on a contour
 * @param glyph the glyph as placed on a straight baseline, its position is relative to the start of the text
 * @param transform the transform of the outline of the glyph, replacing [TypesetGlyph.transform]
 */
class ContourGlyph(val glyph: TypesetGlyph, val transform: Matrix44)

/**
 * Text placed on a contour by [layoutTextOnContour]
 * @param glyphs the glyphs that are placed
 * @param start the distance along the baseline where the text starts, after anchoring
 * @param advance the advance of the text along the baseline
 */
class ContourTextLayout(val glyphs: List<ContourGlyph>, val start: Double, val advance: Double) {
    /** the distance along the baseline where the text ends, where text that continues it starts */
    val end: Double get() = start + advance
}

/**
 * Places [spans] on a single line along [baseline]. The text is shaped as a single paragraph, so it is ordered and
 * shaped across spans; newlines are treated as spaces. The size, width factor and baseline shift of the style of
 * every span are applied, like [typesetText] does.
 *
 * Every span can shift the text from its first character on, like the dx and dy attributes of SVG text: the x
 * component of its shift moves the text along the baseline, the y component moves it across the baseline, away from
 * the ascenders for positive values. Shifts accumulate, a shift applies to the text of its span and to all text that
 * follows it. In mixed-direction text shifts are applied in visual order, at the word that holds the first character
 * of the span.
 *
 * @param style the style that is cascaded into the style of every span
 * @param startDistance the distance along the baseline at which the text is anchored
 * @param anchor the point of the text at [startDistance], 0.0 for the start, 0.5 for the middle, 1.0 for the end
 * @param overflow what happens to glyphs beyond the start or the end of the baseline
 * @param pivot the point of every glyph that is placed on the baseline
 * @param shifts the shift of every span, or an empty list
 */
fun layoutTextOnContour(
    spans: List<TextSpan>,
    baseline: ContourBaseline,
    style: TextStyle = TextStyle.Defaults,
    startDistance: Double = 0.0,
    anchor: Double = 0.0,
    overflow: ContourOverflow = ContourOverflow.WRAP,
    pivot: GlyphPivot = GlyphPivot.START,
    shifts: List<Vector2> = emptyList(),
    shaper: TextShapingDriver = TextShapingDriver.instance
): ContourTextLayout {
    require(shifts.isEmpty() || shifts.size == spans.size) { "there should be a shift for every span" }
    val paragraph = shapeParagraph(
        spans.map { it.copy(text = it.text.replace('\n', ' ')) }, style, style.direction,
        hyphenate = false, shaper = shaper
    )

    // the shifts by the offset of the first character of their span, spans without text share an offset
    val shiftsByOffset = mutableMapOf<Int, Vector2>()
    var spanOffset = 0
    for ((span, shift) in spans.zip(shifts)) {
        shiftsByOffset[spanOffset] = (shiftsByOffset[spanOffset] ?: Vector2.ZERO) + shift
        spanOffset += span.text.length
    }

    // the glyphs on a straight baseline and their advances, the y component of the cursor is the accumulated shift
    // across the baseline
    val glyphs = mutableListOf<Pair<TypesetGlyph, Double>>()
    var cursor = Vector2.ZERO
    for (index in paragraph.visualOrder(0, paragraph.items.size)) {
        val textStart = paragraph.textStart(index)
        if (shiftsByOffset.isNotEmpty() && textStart >= 0) {
            for (offset in textStart until textStart + paragraph.textLength(index)) {
                shiftsByOffset[offset]?.let { cursor += it }
            }
        }
        when (val item = paragraph.items[index]) {
            is LineBox -> {
                val wordStyle = item.content.style
                val wordFace = wordStyle.face ?: error("face not set")
                val sizeInEm = wordStyle.sizeInEm ?: error("sizeInEm not set")
                val scale = sizeInEm * (wordStyle.textWidthFactor ?: error("textWidthFactor not set"))
                val baselineShift = Vector2(0.0, -(wordStyle.baselineShiftInEm ?: 0.0) * wordFace.height * sizeInEm)
                for (sr: ShapeResult in item.content.shapeResults) {
                    val glyph = TypesetGlyph(wordFace, sr.glyphIndex, cursor + sr.offset * scale + baselineShift, wordStyle)
                    glyphs.add(glyph to sr.advance.x * scale)
                    cursor += sr.advance * scale
                }
            }

            is LineGlue -> cursor += Vector2(item.width, 0.0)
            is LinePenalty -> {}
        }
    }

    val advance = cursor.x
    val start = startDistance - advance * anchor
    val length = baseline.length
    if (length <= 0.0) {
        return ContourTextLayout(emptyList(), start, advance)
    }

    val placed = glyphs.mapNotNull { (glyph, glyphAdvance) ->
        val pivotOffset = if (pivot == GlyphPivot.MIDDLE) glyphAdvance / 2.0 else 0.0
        val distance = start + glyph.position.x + pivotOffset
        val onBaseline = when (overflow) {
            ContourOverflow.WRAP -> if (baseline.closed) distance.mod(length) else distance.coerceIn(0.0, length)
            ContourOverflow.CLAMP -> distance.coerceIn(0.0, length)
            ContourOverflow.HIDE -> if (distance in 0.0..length) distance else return@mapNotNull null
        }
        val sizeInEm = glyph.style.sizeInEm ?: error("sizeInEm not set")
        val textWidthFactor = glyph.style.textWidthFactor ?: error("textWidthFactor not set")
        val transform = baseline.pose(onBaseline) * transform {
            translate(-pivotOffset, glyph.position.y)
            scale(sizeInEm * textWidthFactor, sizeInEm, 1.0)
        }
        ContourGlyph(glyph, transform)
    }
    return ContourTextLayout(placed, start, advance)
}
