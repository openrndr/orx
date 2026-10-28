package org.openrndr.extra.textwriter

import org.openrndr.draw.font.internal.BidiText
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.TextShapingDriver

private const val SOFT_HYPHEN = '\u00AD'

/**
 * A text shaped into Knuth-Plass [items], together with the bidirectional analysis of the text that is used to
 * reorder lines.
 *
 * @param bidi the bidirectional analysis of the concatenated text of all spans
 * @param items the paragraph items in logical order, ends with a forced break
 */
class ShapedParagraph internal constructor(
    val bidi: BidiText,
    val items: List<LineItem<ShapedWord<TextStyle>>>,
    private val itemStarts: IntArray
) {
    // the item each UTF-16 unit of the text belongs to, -1 for newlines
    private val itemOfChar = IntArray(bidi.text.length) { -1 }.also { itemOfChar ->
        for (i in items.indices) {
            val start = itemStarts[i]
            if (start >= 0) itemOfChar.fill(i, start, start + itemLength(i))
        }
    }

    private fun itemLength(index: Int) = when (val item = items[index]) {
        is LineBox -> item.content.text.length
        is LineGlue -> 1
        is LinePenalty -> 0
    }

    /**
     * Returns the indices of the items in [start] until [end] (exclusive) in visual order, from left to right.
     * Items without text, like penalties, are left out.
     */
    fun visualOrder(start: Int, end: Int): IntArray {
        var textStart = Int.MAX_VALUE
        var textEnd = -1
        for (i in start until end) {
            val itemStart = itemStarts[i]
            if (itemStart < 0) continue
            textStart = minOf(textStart, itemStart)
            textEnd = maxOf(textEnd, itemStart + itemLength(i))
        }
        if (textEnd < 0) return IntArray(0)

        // Every item has a uniform level, so its units are adjacent in the visual order of the line
        val order = mutableListOf<Int>()
        val seen = BooleanArray(end - start)
        for (c in bidi.visualOrder(textStart, textEnd)) {
            val item = itemOfChar[c]
            if (item < start || item >= end || seen[item - start]) continue
            seen[item - start] = true
            order.add(item)
        }
        return order.toIntArray()
    }
}

/**
 * Splits the text of [spans] into words, spaces and newlines and shapes the words. Spaces become glue, newlines
 * become forced breaks and words become [LineBox]es holding a [ShapedWord].
 *
 * The concatenated text of all spans is analyzed with [BidiTextDriver] so that the direction of neutrals between
 * words is resolved using the whole paragraph. Words are split into runs of uniform direction and script, every run
 * is shaped with its own direction and script and becomes a box, consecutive boxes are never broken apart.
 * Soft hyphens (U+00AD) become hyphenation points: flagged penalties with the width of a hyphen, the hyphen is
 * stored with the preceding word in [ShapedWord.hyphen]. Words in spans with a [TextStyle.language] are hyphenated
 * automatically when [TextStyle.hyphenate] is enabled, using [Hyphenator].
 *
 * @param style the style that is cascaded into the style of every span
 * @param direction the paragraph direction, null to determine it from the first strong character of every paragraph
 * @param hyphenate false to ignore hyphenation, for text that is not broken into lines
 */
fun shapeParagraph(
    spans: List<TextSpan>,
    style: TextStyle,
    direction: Direction?,
    hyphenate: Boolean = true,
    shaper: TextShapingDriver = TextShapingDriver.instance,
    bidiDriver: BidiTextDriver = BidiTextDriver.instance
): ShapedParagraph {
    val text = spans.joinToString("") { it.text }
    val bidi = bidiDriver.analyze(text, direction)

    val items = mutableListOf<LineItem<ShapedWord<TextStyle>>>()
    val itemStarts = mutableListOf<Int>()
    fun addForcedBreak() {
        items.appendForcedBreak()
        while (itemStarts.size < items.size) itemStarts.add(-1)
    }

    var offset = 0
    for (span in spans) {
        val spanStyle = style.cascade(span.style)
        val face = spanStyle.face ?: error("face not set in span")
        val sizeInEm = spanStyle.sizeInEm ?: error("sizeInEm not set in span")
        val scale = sizeInEm * (spanStyle.textWidthFactor ?: error("textWidthFactor not set in span"))
        val spaceWidth = face.glyphForCharacter(' ').advanceWidth() * scale
        val language = spanStyle.language
        // the shaper only guesses direction and script when none of direction, script and language are given
        val hyphen by lazy {
            shaper.shape(face, "-", spanStyle.features, Direction.LEFT_TO_RIGHT, Script.COMMON, language)
        }
        val autoHyphenate = hyphenate && language != null && spanStyle.hyphenate != false

        fun addBoxes(start: Int, end: Int, hyphenated: Boolean) {
            val runs = bidi.runs(start, end)
            for ((index, run) in runs.withIndex()) {
                val runText = text.substring(run.start, run.end)
                val shaped = shaper.shape(face, runText, spanStyle.features, run.direction, run.script, language)
                val width = shaped.sumOf { it.advance.x } * scale
                val rightToLeft = run.direction == Direction.RIGHT_TO_LEFT
                val runHyphen = if (hyphenated && index == runs.lastIndex) hyphen else emptyList()
                items.add(LineBox(width, ShapedWord(runText, shaped, spanStyle, rightToLeft, runHyphen)))
                itemStarts.add(run.start)
            }
        }

        // Soft hyphens and hyphenation points split a word into parts, a line can break between parts at the cost of
        // a hyphen
        fun addWord(start: Int, end: Int) {
            val points = if (autoHyphenate) {
                Hyphenator.forLanguage(language!!, bidi.scriptAt(start))
                    ?.textHyphenationPoints(text.substring(start, end))?.map { start + it }?.toSet()
            } else null

            val parts = mutableListOf<IntRange>()
            var partStart = start
            for (c in start..end) {
                if (c == end || text[c] == SOFT_HYPHEN) {
                    if (c > partStart) parts.add(partStart until c)
                    partStart = c + 1
                } else if (points != null && c in points && c > partStart) {
                    parts.add(partStart until c)
                    partStart = c
                }
            }
            for ((index, part) in parts.withIndex()) {
                val hyphenated = index < parts.lastIndex
                addBoxes(part.first, part.last + 1, hyphenated)
                if (hyphenated) {
                    items.add(LinePenalty(hyphen.sumOf { it.advance.x } * scale, LinePenalty.HYPHEN, true))
                    itemStarts.add(-1)
                }
            }
        }

        val spanText = span.text
        var i = 0
        while (i < spanText.length) {
            when (spanText[i]) {
                ' ', '\t' -> {
                    items.add(LineGlue(spaceWidth, spaceWidth / 2.0, spaceWidth / 3.0))
                    itemStarts.add(offset + i)
                    i++
                }

                '\n' -> {
                    addForcedBreak()
                    i++
                }

                else -> {
                    var j = i
                    while (j < spanText.length && spanText[j] != ' ' && spanText[j] != '\t' && spanText[j] != '\n') j++
                    addWord(offset + i, offset + j)
                    i = j
                }
            }
        }
        offset += spanText.length
    }
    addForcedBreak()
    return ShapedParagraph(bidi, items, itemStarts.toIntArray())
}
