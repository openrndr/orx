package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.StrokeMode
import org.openrndr.draw.slug.TextSpan
import org.openrndr.draw.slug.TextStyle
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
import kotlin.math.abs

private class KPBox(val width: Double, val shapeResults: List<ShapeResult>, val style: TextStyle)
private class KPGlue(val width: Double, val stretch: Double, val shrink: Double)
private class KPPenalty(val width: Double, val penalty: Double, val flagged: Boolean)

private sealed class KPItem {
    class Box(val box: KPBox) : KPItem()
    class Glue(val glue: KPGlue) : KPItem()
    class Penalty(val penalty: KPPenalty) : KPItem()
}

private class KPBreakpoint(
    val index: Int,
    val demerits: Double,
    val line: Int,
    val totalWidth: Double,
    val totalStretch: Double,
    val totalShrink: Double,
    val previous: KPBreakpoint?
)

/**
 * Draws [spans] along [contours] using the Knuth-Plass line breaking algorithm.
 *
 * Every contour acts as a single line, the length of the contour is used as the line width.
 * Text that does not fit on a contour overflows to the next contour, text that does not fit
 * on any of the contours is discarded.
 *
 * @param baseStyle the style that is cascaded into the style of every span
 * @param contours the contours on which the text is placed, in order
 * @param startT the normalized position on every contour at which the text starts
 */
fun slugTextOnContours(
    drawer: Drawer,
    slugDrawer: SlugDrawer,
    slugGlyphMap: SlugGlyphMap,
    baseStyle: TextStyle,
    spans: List<TextSpan>,
    contours: List<RectifiedContour>,
    startT: Double = 0.0
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

    val items = mutableListOf<KPItem>()

    for (span in spans) {
        val spanStyle = baseStyle.cascade(span.style)
        val face = spanStyle.face ?: error("face not set in span")

        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (ch in span.text) {
            when (ch) {
                ' ', '\t' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add(" ")
                }

                '\n' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add("\n")
                }

                else -> {
                    currentWord.append(ch)
                }
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        val spanTextWidthFactor = spanStyle.textWidthFactor ?: error("textWidthFactor not set in span")
        val spanSizeInEm = spanStyle.sizeInEm ?: error("sizeInEm not set in span")

        val spaceGlyph = face.glyphForCharacter(' ')
        val spaceWidth = spaceGlyph.advanceWidth() * spanTextWidthFactor * spanSizeInEm

        for (word in words) {
            when (word) {
                " " -> {
                    items.add(KPItem.Glue(KPGlue(spaceWidth, spaceWidth / 2.0, spaceWidth / 3.0)))
                }

                "\n" -> {
                    items.add(KPItem.Penalty(KPPenalty(0.0, Double.POSITIVE_INFINITY, false)))
                    items.add(KPItem.Glue(KPGlue(0.0, 10000.0, 0.0)))
                    items.add(KPItem.Penalty(KPPenalty(0.0, -10000.0, false)))
                }

                else -> {
                    val features = spanStyle.features

                    val shaped = shaper.shape(face, word, features)
                    var wordWidth = 0.0
                    for (sr in shaped) {
                        wordWidth += sr.advance.x * spanTextWidthFactor * spanSizeInEm
                    }
                    items.add(KPItem.Box(KPBox(wordWidth, shaped, spanStyle)))
                }
            }
        }
    }

    // Add finishing penalty (forced break at end)
    items.add(KPItem.Penalty(KPPenalty(0.0, Double.POSITIVE_INFINITY, false)))
    items.add(KPItem.Glue(KPGlue(0.0, 10000.0, 0.0)))
    items.add(KPItem.Penalty(KPPenalty(0.0, -10000.0, false)))

    // Every contour holds exactly one line, its length is the width of that line
    val lineWidths = contours.map { it.contour.length }
    val defaultLineWidth = lineWidths.first()

    val cumWidth = DoubleArray(items.size + 1)
    val cumStretch = DoubleArray(items.size + 1)
    val cumShrink = DoubleArray(items.size + 1)

    for (i in items.indices) {
        cumWidth[i + 1] = cumWidth[i] + when (val item = items[i]) {
            is KPItem.Box -> item.box.width
            is KPItem.Glue -> item.glue.width
            is KPItem.Penalty -> 0.0
        }
        cumStretch[i + 1] = cumStretch[i] + when (val item = items[i]) {
            is KPItem.Glue -> item.glue.stretch
            else -> 0.0
        }
        cumShrink[i + 1] = cumShrink[i] + when (val item = items[i]) {
            is KPItem.Glue -> item.glue.shrink
            else -> 0.0
        }
    }

    val activeBreakpoints = mutableListOf(
        KPBreakpoint(0, 0.0, 0, 0.0, 0.0, 0.0, null)
    )

    fun lineWidthForLine(lineIndex: Int): Double {
        return if (lineIndex < lineWidths.size) lineWidths[lineIndex] else defaultLineWidth
    }

    fun computeAdjustmentRatio(bp: KPBreakpoint, itemIndex: Int): Double {
        val lw = lineWidthForLine(bp.line)
        val w = cumWidth[itemIndex] - bp.totalWidth
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

    fun computeDemerits(penalty: Double, r: Double): Double {
        val badness = if (r < -1.0) 10000.0 else 100.0 * abs(r).let { it * it * it }
        return if (penalty >= 0) {
            (1.0 + badness + penalty) * (1.0 + badness + penalty)
        } else if (penalty > -10000.0) {
            (1.0 + badness) * (1.0 + badness) - penalty * penalty
        } else {
            (1.0 + badness) * (1.0 + badness)
        }
    }

    for (i in items.indices) {
        val item = items[i]
        if (item is KPItem.Penalty && item.penalty.penalty >= 10000.0) continue
        if (item !is KPItem.Penalty && item !is KPItem.Glue) continue
        if (item is KPItem.Glue && (i == 0 || items[i - 1] !is KPItem.Box)) continue

        val breakIndex = i
        val newWidth = cumWidth[breakIndex + 1]
        val newStretch = cumStretch[breakIndex + 1]
        val newShrink = cumShrink[breakIndex + 1]

        val toRemove = mutableListOf<KPBreakpoint>()
        var bestCandidate: KPBreakpoint? = null
        var bestDemerits = Double.MAX_VALUE

        for (bp in activeBreakpoints) {
            val r = computeAdjustmentRatio(bp, breakIndex)

            if (r < -1.0 || (item is KPItem.Penalty && item.penalty.penalty == -10000.0)) {
                toRemove.add(bp)
            }

            if (r >= -1.0 && r <= 10000.0) {
                val penalty = if (item is KPItem.Penalty) item.penalty.penalty else 0.0
                val d = bp.demerits + computeDemerits(penalty, r)
                if (d < bestDemerits) {
                    bestDemerits = d
                    bestCandidate = bp
                }
            }
        }

        activeBreakpoints.removeAll(toRemove)

        if (bestCandidate != null) {
            activeBreakpoints.add(
                KPBreakpoint(
                    breakIndex,
                    bestDemerits,
                    bestCandidate.line + 1,
                    newWidth,
                    newStretch,
                    newShrink,
                    bestCandidate
                )
            )
        }
    }

    val finalBp = activeBreakpoints.minByOrNull { it.demerits } ?: return

    val bpChain = mutableListOf<KPBreakpoint>()
    var bp: KPBreakpoint? = finalBp
    while (bp != null) {
        bpChain.add(bp)
        bp = bp.previous
    }
    bpChain.reverse()

    val commands = mutableListOf<SlugCommand>()

    for (lineIdx in 0 until bpChain.size - 1) {
        // lines that do not fit on any of the contours are discarded
        if (lineIdx >= contours.size) break

        val contour = contours[lineIdx]
        val lineWidth = lineWidths[lineIdx]
        val closed = contour.contour.closed
        val dt = if (lineWidth > 0.0) 1.0 / lineWidth else 0.0

        val startBp = bpChain[lineIdx]
        val endBp = bpChain[lineIdx + 1]
        val start = startBp.index
        val end = endBp.index

        val r = computeAdjustmentRatio(startBp, end)
        val isLastLine = lineIdx == bpChain.size - 2

        // Skip leading glue at the start of each line (after a line break)
        var lineStart = start
        if (lineIdx > 0) {
            while (lineStart < end && items[lineStart] is KPItem.Glue) {
                lineStart++
            }
        }

        fun adjustedGlueWidth(glue: KPGlue): Double {
            return if (!justify || isLastLine) {
                if (r < 0) glue.width + r * glue.shrink else glue.width
            } else if (r >= 0) {
                glue.width + r * glue.stretch
            } else {
                glue.width + r * glue.shrink
            }
        }

        val naturalLineWidth = run {
            var w = 0.0
            for (idx in lineStart until end) {
                when (val itm = items[idx]) {
                    is KPItem.Box -> w += itm.box.width
                    is KPItem.Glue -> w += adjustedGlueWidth(itm.glue)
                    is KPItem.Penalty -> {}
                }
            }
            w
        }

        var cursor = (lineWidth - naturalLineWidth) * horizontalAlign

        for (idx in lineStart until end) {
            when (val itm = items[idx]) {
                is KPItem.Box -> {
                    val boxStyle = itm.box.style
                    val boxFace = boxStyle.face ?: error("face not set")
                    val boxSizeInEm = boxStyle.sizeInEm ?: error("sizeInEm not set")
                    val hscale = boxSizeInEm * (boxStyle.textWidthFactor ?: error("textWidthFactor not set"))

                    val itemTransform = transform {
                        scale(hscale, boxSizeInEm)
                    }

                    for (sr in itm.box.shapeResults) {
                        slugGlyphMap.getSlugForGlyphIndex(boxFace, sr.glyphIndex)
                    }
                    for (sr in itm.box.shapeResults) {
                        val slugIndex = slugGlyphMap.getSlugForGlyphIndex(boxFace, sr.glyphIndex)
                        val rawT = startT + (cursor + sr.offset.x * hscale) * dt
                        val t = if (closed) rawT.mod(1.0) else rawT.coerceIn(0.0, 1.0)

                        val pose = contour.pose(t) * itemTransform * fixOrientation

                        commands.add(
                            SlugCommand(
                                slugIndex,
                                pose,
                                boxStyle.fill ?: ColorRGBa.TRANSPARENT,
                                boxStyle.stroke ?: ColorRGBa.TRANSPARENT,
                                strokeWeight = boxStyle.strokeWeight ?: 0.0,
                                (boxStyle.strokeMode ?: StrokeMode.CENTER).mode
                            )
                        )
                        cursor += sr.advance.x * hscale
                    }
                }

                is KPItem.Glue -> {
                    cursor += adjustedGlueWidth(itm.glue)
                }

                is KPItem.Penalty -> { /* skip */
                }
            }
        }
    }

    slugDrawer.prepare(slugGlyphMap.slugMap, commands)
    slugDrawer.draw(drawer, slugGlyphMap.slugMap)
}
