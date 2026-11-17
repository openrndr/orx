package org.openrndr.extra.textwriter

import org.openrndr.draw.font.internal.ShapeResult
import kotlin.math.abs

/**
 * An item in a Knuth-Plass paragraph: a [LineBox] (unbreakable content), a [LineGlue] (stretchable space)
 * or a [LinePenalty] (a possible break with a cost).
 * @param T the type of the content carried by boxes
 */
sealed interface LineItem<out T> {
    val width: Double
}

/**
 * The fraction of the line width under which the last line of a ragged paragraph costs more, see [breakLines]
 */
const val RAGGED_SHORT_LAST_LINE = 0.25

/** Unbreakable content of a given [width]. */
class LineBox<out T>(override val width: Double, val content: T) : LineItem<T>

/** Space that can [stretch] or [shrink]; a line may break at a glue that directly follows a box. */
class LineGlue(override val width: Double, val stretch: Double, val shrink: Double) : LineItem<Nothing> {
    /**
     * The width of this glue on a line with adjustment ratio [ratio].
     * Unjustified lines and the last line only shrink, so they never get wider than their natural width.
     */
    fun adjustedWidth(ratio: Double, justify: Boolean, isLastLine: Boolean): Double {
        return if (!justify || isLastLine) {
            if (ratio < 0) width + ratio * shrink else width
        } else if (ratio >= 0) {
            width + ratio * stretch
        } else {
            width + ratio * shrink
        }
    }
}

/** A possible break; a [penalty] of [LinePenalty.FORBIDDEN] or more forbids, [LinePenalty.FORCED] or less forces a break. */
class LinePenalty(override val width: Double, val penalty: Double, val flagged: Boolean) : LineItem<Nothing> {
    companion object {
        const val FORBIDDEN = 10000.0
        const val FORCED = -10000.0

        /** The penalty of a break at a hyphenation point */
        const val HYPHEN = 50.0

        /** Additional demerits for two consecutive lines that end at a flagged penalty */
        const val CONSECUTIVE_FLAGGED_DEMERITS = 3000.0

        /** Additional demerits for a last line of a paragraph with nothing on it, see breakLines' shortLastLine */
        const val SHORT_LAST_LINE_DEMERITS = 1000000.0
    }
}

/**
 * A [text] shaped with [style]; the content of the boxes created by [shapeParagraph].
 * @param rightToLeft true when the text was shaped right-to-left
 * @param hyphen the shaped hyphen that is shown after the text when a line breaks at the hyphenation point that
 * directly follows this word, empty when no hyphenation point follows
 */
class ShapedWord<out S>(
    val text: String,
    val shapeResults: List<ShapeResult>,
    val style: S,
    val rightToLeft: Boolean = false,
    val hyphen: List<ShapeResult> = emptyList()
)

/**
 * A line as found by [breakLines].
 * @param start index of the first item of the line, leading glue is already skipped
 * @param end index of the break item that ends the line (exclusive)
 * @param ratio the adjustment ratio of the line, negative when the line has to shrink
 * @param isLast true for the last line of the paragraph
 * @param hyphenated true when the line ends at a flagged penalty, a hyphenation point
 */
class BrokenLine(val start: Int, val end: Int, val ratio: Double, val isLast: Boolean, val hyphenated: Boolean)

/**
 * Appends a forced break (and the filler glue before it) to the paragraph.
 * Every paragraph should end with one.
 */
fun <T> MutableList<LineItem<T>>.appendForcedBreak() {
    add(LinePenalty(0.0, Double.POSITIVE_INFINITY, false))
    add(LineGlue(0.0, LinePenalty.FORBIDDEN, 0.0))
    add(LinePenalty(0.0, LinePenalty.FORCED, false))
}

private class Breakpoint(
    val index: Int,
    val demerits: Double,
    val line: Int,
    val totalWidth: Double,
    val totalStretch: Double,
    val totalShrink: Double,
    val flagged: Boolean,
    val previous: Breakpoint?
)

/**
 * Breaks a paragraph into lines using the Knuth-Plass algorithm.
 *
 * @param items the paragraph, should end with [appendForcedBreak]
 * @param shortLastLine for ragged text: a last line of a paragraph that is shorter than this fraction of its width costs
 * as much more as it is shorter, so that a word is not left alone on it; 0.0 for none, as for justified text
 * @param lineWidth the available width for the line with the given index
 * @return the lines in order, lines that cannot be broken to fit are overfull
 */
fun breakLines(items: List<LineItem<*>>, shortLastLine: Double = 0.0, lineWidth: (lineIndex: Int) -> Double): List<BrokenLine> {
    val cumWidth = DoubleArray(items.size + 1)
    val cumStretch = DoubleArray(items.size + 1)
    val cumShrink = DoubleArray(items.size + 1)

    for (i in items.indices) {
        val item = items[i]
        cumWidth[i + 1] = cumWidth[i] + if (item is LinePenalty) 0.0 else item.width
        cumStretch[i + 1] = cumStretch[i] + if (item is LineGlue) item.stretch else 0.0
        cumShrink[i + 1] = cumShrink[i] + if (item is LineGlue) item.shrink else 0.0
    }

    fun adjustmentRatio(bp: Breakpoint, itemIndex: Int): Double {
        val lw = lineWidth(bp.line)
        // a line that ends at a penalty includes the width of the penalty, for example a hyphen
        val penaltyWidth = (items.getOrNull(itemIndex) as? LinePenalty)?.width ?: 0.0
        val w = cumWidth[itemIndex] - bp.totalWidth + penaltyWidth
        return if (w < lw) {
            val stretch = cumStretch[itemIndex] - bp.totalStretch
            if (stretch > 0) (lw - w) / stretch else 10000.0
        } else if (w > lw) {
            val shrink = cumShrink[itemIndex] - bp.totalShrink
            if (shrink > 0) (lw - w) / shrink else -10000.0
        } else {
            0.0
        }
    }

    fun demerits(penalty: Double, r: Double): Double {
        val badness = if (r < -1.0) 10000.0 else 100.0 * abs(r).let { it * it * it }
        return if (penalty >= 0) {
            (1.0 + badness + penalty) * (1.0 + badness + penalty)
        } else if (penalty > LinePenalty.FORCED) {
            (1.0 + badness) * (1.0 + badness) - penalty * penalty
        } else {
            (1.0 + badness) * (1.0 + badness)
        }
    }

    /** the demerits of a short last line of a paragraph, from [bp] to the forced break at [itemIndex] */
    fun shortLineDemerits(bp: Breakpoint, itemIndex: Int): Double {
        val item = items[itemIndex]
        if (shortLastLine <= 0.0 || item !is LinePenalty || item.penalty > LinePenalty.FORCED) return 0.0
        val wanted = shortLastLine * lineWidth(bp.line)
        val w = cumWidth[itemIndex] - bp.totalWidth
        return if (w >= wanted || wanted <= 0.0) 0.0 else LinePenalty.SHORT_LAST_LINE_DEMERITS * (wanted - w) / wanted
    }

    /** the totals at the start of the line after a break at [index]: the glue and penalties there are left out */
    fun lineStart(index: Int): Int {
        var j = index + 1
        while (j < items.size && (items[j] is LineGlue || (items[j] as? LinePenalty)?.let { it.penalty > LinePenalty.FORCED } == true)) j++
        return j
    }

    val activeBreakpoints = mutableListOf(Breakpoint(0, 0.0, 0, 0.0, 0.0, 0.0, false, null))

    for (i in items.indices) {
        val item = items[i]
        if (item is LinePenalty && item.penalty >= LinePenalty.FORBIDDEN) continue
        if (item !is LinePenalty && item !is LineGlue) continue
        if (item is LineGlue && (i == 0 || items[i - 1] !is LineBox<*>)) continue

        val toRemove = mutableListOf<Breakpoint>()
        var bestCandidate: Breakpoint? = null
        var bestDemerits = Double.MAX_VALUE

        for (bp in activeBreakpoints) {
            val r = adjustmentRatio(bp, i)

            if (r < -1.0 || (item is LinePenalty && item.penalty == LinePenalty.FORCED)) {
                toRemove.add(bp)
            }

            if (r >= -1.0 && r <= 10000.0) {
                val penalty = if (item is LinePenalty) item.penalty else 0.0
                val flaggedDemerits =
                    if (item is LinePenalty && item.flagged && bp.flagged) LinePenalty.CONSECUTIVE_FLAGGED_DEMERITS else 0.0
                val d = bp.demerits + demerits(penalty, r) + flaggedDemerits + shortLineDemerits(bp, i)
                if (d < bestDemerits) {
                    bestDemerits = d
                    bestCandidate = bp
                }
            }
        }

        activeBreakpoints.removeAll(toRemove)

        // When no feasible break is left, accept an overfull line instead of losing the rest of the paragraph
        if (bestCandidate == null && activeBreakpoints.isEmpty() && toRemove.isNotEmpty()) {
            val penalty = if (item is LinePenalty) item.penalty else 0.0
            bestCandidate = toRemove.minBy { it.demerits }
            bestDemerits = bestCandidate.demerits + demerits(penalty, -1.0)
        }

        if (bestCandidate != null) {
            activeBreakpoints.add(
                Breakpoint(
                    i, bestDemerits, bestCandidate.line + 1,
                    cumWidth[lineStart(i)], cumStretch[lineStart(i)], cumShrink[lineStart(i)],
                    item is LinePenalty && item.flagged,
                    bestCandidate
                )
            )
        }
    }

    val finalBp = activeBreakpoints.minByOrNull { it.demerits } ?: return emptyList()

    val chain = mutableListOf<Breakpoint>()
    var bp: Breakpoint? = finalBp
    while (bp != null) {
        chain.add(bp)
        bp = bp.previous
    }
    chain.reverse()

    return (0 until chain.size - 1).map { lineIdx ->
        val startBp = chain[lineIdx]
        val end = chain[lineIdx + 1].index

        // a line after a break starts after the break and the glue and penalties that follow it
        val start = if (lineIdx > 0) minOf(lineStart(startBp.index), end) else startBp.index
        val breakItem = items[end]
        BrokenLine(
            // overfull lines shrink as much as possible but no further
            start, end, adjustmentRatio(startBp, end).coerceAtLeast(-1.0), lineIdx == chain.size - 2,
            breakItem is LinePenalty && breakItem.flagged
        )
    }
}
