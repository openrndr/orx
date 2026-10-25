package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.extra.textwriter.StrokeMode
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.textwriter.LineBox
import org.openrndr.extra.textwriter.LineGlue
import org.openrndr.extra.textwriter.LineItem
import org.openrndr.extra.textwriter.LinePenalty
import org.openrndr.extra.textwriter.BrokenLine
import org.openrndr.extra.textwriter.breakLines
import org.openrndr.extra.textwriter.shapeParagraph
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
import kotlin.math.roundToInt

/**
 * Draws [spans] along [contours] using the Knuth-Plass line breaking algorithm.
 *
 * Every contour acts as a single line, the length of the contour is used as the line width.
 * Text that does not fit on a contour overflows to the next contour, text that does not fit
 * on any of the contours is discarded.
 *
 * The vertical alignment of [baseStyle] determines the contours that are used when the text does not need all of
 * them: the text is moved to later contours by the vertical alignment times the largest number of contours it can
 * be moved without less text fitting, rounded to a whole contour. With a vertical alignment of 0 the text starts at
 * the first contour, with 1 it ends at the last contour.
 *
 * @param baseStyle the style that is cascaded into the style of every span, layout properties
 * (alignment, justification, direction) are taken from this style
 * @param contours the contours on which the text is placed, in order
 * @param startT the normalized position on every contour at which the text starts
 * @param baselineOffset the distance between the contour and the baseline of the text, positive distances move the
 * text towards its ascenders. Glyph spacing and line widths are measured on the offset baseline.
 */
fun slugTextOnContours(
    drawer: Drawer,
    slugDrawer: SlugDrawer,
    slugGlyphMap: SlugGlyphMap,
    baseStyle: TextStyle,
    spans: List<TextSpan>,
    contours: List<RectifiedContour>,
    startT: Double = 0.0,
    baselineOffset: Double = 0.0
) {
    require(contours.isNotEmpty()) { "contours must not be empty" }
    if (spans.isEmpty()) {
        return
    }

    val fixOrientation = Matrix44.fromColumnVectors(
        Vector4(-1.0, 0.0, 0.0, 0.0),
        Vector4(0.0, -1.0, 0.0, 0.0),
        Vector4.UNIT_Z,
        Vector4.UNIT_W
    )

    val shaper = TextShapingDriver.instance

    val justify = baseStyle.justify ?: error("justify not set")
    val horizontalAlign = baseStyle.horizontalAlign ?: error("horizontalAlign not set")

    val paragraph = shapeParagraph(spans, baseStyle, baseStyle.direction, shaper = shaper)
    val items = paragraph.items

    // Every contour holds exactly one line, its length is the width of that line
    val baselines = contours.map { BaselinePath(it, baselineOffset) }
    val lineWidths = baselines.map { it.length }
    val (firstContour, lines) = contourLines(items, lineWidths, baseStyle.verticalAlign ?: 0.0)

    val commands = mutableListOf<SlugCommand>()

    // lines that do not fit on any of the contours are discarded
    for ((lineIdx, line) in lines.withIndex()) {
        val baseline = baselines[firstContour + lineIdx]
        val lineWidth = baseline.length
        val du = if (lineWidth > 0.0) 1.0 / lineWidth else 0.0

        fun glueWidth(glue: LineGlue) = glue.adjustedWidth(line.ratio, justify, line.isLast)

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

        var cursor = (lineWidth - naturalLineWidth) * horizontalAlign

        for (idx in paragraph.visualOrder(line.start, line.end)) {
            when (val item = items[idx]) {
                is LineBox -> {
                    val wordStyle = item.content.style
                    val wordFace = wordStyle.face ?: error("face not set")
                    val sizeInEm = wordStyle.sizeInEm ?: error("sizeInEm not set")
                    val hscale = sizeInEm * (wordStyle.textWidthFactor ?: error("textWidthFactor not set"))

                    // the baseline shift moves the word towards its ascenders, the y axis of the pose
                    val baselineShift = (wordStyle.baselineShiftInEm ?: 0.0) * wordFace.height * sizeInEm
                    val itemTransform = transform {
                        translate(0.0, baselineShift)
                        scale(hscale, sizeInEm)
                    }

                    // the hyphen goes at the end of the word, which is on the left for right-to-left words
                    val hyphen = if (idx == hyphenatedItem) item.content.hyphen else emptyList()
                    val shapeResults = if (item.content.rightToLeft) hyphen + item.content.shapeResults
                    else item.content.shapeResults + hyphen

                    for (sr in shapeResults) {
                        slugGlyphMap.getSlugForGlyphIndex(wordFace, sr.glyphIndex)
                    }
                    for (sr in shapeResults) {
                        val slugIndex = slugGlyphMap.getSlugForGlyphIndex(wordFace, sr.glyphIndex)
                        val pose = baseline.pose(startT + (cursor + sr.offset.x * hscale) * du) * itemTransform *
                                fixOrientation

                        commands.add(
                            SlugCommand(
                                slugIndex,
                                pose,
                                wordStyle.fill ?: ColorRGBa.TRANSPARENT,
                                wordStyle.stroke ?: ColorRGBa.TRANSPARENT,
                                strokeWeight = wordStyle.strokeWeight ?: 0.0,
                                (wordStyle.strokeMode ?: StrokeMode.CENTER).mode
                            )
                        )
                        cursor += sr.advance.x * hscale
                    }
                }

                is LineGlue -> cursor += glueWidth(item)
                is LinePenalty -> {}
            }
        }
    }

    slugDrawer.prepare(slugGlyphMap.slugMap, commands)
    slugDrawer.draw(drawer, slugGlyphMap.slugMap)
}

/**
 * Breaks [items] into lines for contours with [lineWidths], one line per contour. When the text does not need all
 * contours it is moved to later contours by [verticalAlign] times the largest number of contours it can be moved
 * without less text fitting.
 *
 * @return the index of the contour of the first line and the lines that fit, lines that do not fit are left out
 */
internal fun contourLines(
    items: List<LineItem<*>>,
    lineWidths: List<Double>,
    verticalAlign: Double
): Pair<Int, List<BrokenLine>> {
    fun linesFrom(first: Int): List<BrokenLine> {
        val widths = lineWidths.drop(first)
        return breakLines(items) { lineIndex -> widths[minOf(lineIndex, widths.lastIndex)] }.take(widths.size)
    }

    // the index of the item after the last placed line
    fun placed(lines: List<BrokenLine>) = lines.lastOrNull()?.end ?: 0

    val top = linesFrom(0)
    if (verticalAlign == 0.0) return 0 to top

    val required = placed(top)
    val maximumShift = lineWidths.indices.lastOrNull { placed(linesFrom(it)) >= required } ?: 0
    val first = (maximumShift * verticalAlign).roundToInt().coerceIn(0, maximumShift)
    return first to linesFrom(first)
}
