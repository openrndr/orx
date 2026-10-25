package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.extra.textwriter.StrokeMode
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.LineBox
import org.openrndr.extra.textwriter.LineGlue
import org.openrndr.extra.textwriter.LinePenalty
import org.openrndr.extra.textwriter.shapeParagraph
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform

fun RectifiedContour.positionOnlyPose(t: Double): Matrix44 {
    return Matrix44.fromColumnVectors(Vector4.UNIT_X * -1.0, Vector4.UNIT_Y * -1.0, Vector4.UNIT_Z, position(t).xy01)
}

/**
 * Draws [spans] on a single line along [contour] using slugs. Text that is longer than the contour wraps around a
 * closed contour.
 *
 * @param baseStyle the style that is cascaded into the style of every span
 * @param startT the normalized position on the contour at which the text starts
 * @param baselineOffset the distance between the contour and the baseline of the text, positive distances move the
 * text towards its ascenders. Glyph spacing is measured on the offset baseline.
 */
fun slugTextOnContour(
    drawer: Drawer,
    slugDrawer: SlugDrawer,
    slugGlyphMap: SlugGlyphMap,
    baseStyle: TextStyle,
    spans: List<TextSpan>,
    contour: RectifiedContour,
    startT: Double = 0.0,
    baselineOffset: Double = 0.0
) {
    val fixOrientation = Matrix44.fromColumnVectors(
        Vector4(-1.0, 0.0, 0.0, 0.0),
        Vector4(0.0, -1.0, 0.0, 0.0),
        Vector4.UNIT_Z,
        Vector4.UNIT_W
    )

    val shaper = TextShapingDriver.instance

    class WordInfo(val word: String, val width: Double, val shaped: List<ShapeResult>, val style: TextStyle)

    // The whole text is placed on a single line, newlines are treated as spaces
    val paragraph = shapeParagraph(
        spans.map { it.copy(text = it.text.replace('\n', ' ')) }, baseStyle, baseStyle.direction,
        hyphenate = false, shaper = shaper
    )
    val orderedItems = paragraph.visualOrder(0, paragraph.items.size).asList().mapNotNull { index ->
        when (val item = paragraph.items[index]) {
            is LineBox -> WordInfo(item.content.text, item.width, item.content.shapeResults, item.content.style)
            is LineGlue -> WordInfo(" ", item.width, emptyList(), baseStyle)
            is LinePenalty -> null
        }
    }

    var cursorT = startT

    val baseline = BaselinePath(contour, baselineOffset)
    val dt = 1.0 / baseline.length

    val commands = mutableListOf<SlugCommand>()
    for (item in orderedItems) {
        if (item.word == " ") {
            cursorT += item.width * dt
        } else {

            // the baseline shift moves the word towards its ascenders, the y axis of the pose
            val baselineShift = (item.style.baselineShiftInEm ?: 0.0) * item.style.face!!.height * item.style.sizeInEm!!
            val itemTransform = transform {
                translate(0.0, baselineShift)
                scale(item.style.sizeInEm!! * item.style.textWidthFactor!!, item.style.sizeInEm!!)
            }

            val hscale = item.style.sizeInEm!! * item.style.textWidthFactor!!
            for (shape in item.shaped) {
                val slugIndex = slugGlyphMap.getSlugForGlyphIndex(item.style.face!!, shape.glyphIndex)

                val pose = baseline.pose(cursorT + shape.offset.x * hscale * dt) * itemTransform * fixOrientation

                val cmd = SlugCommand(
                    slugIndex,
                    pose,
                    item.style.fill ?: ColorRGBa.TRANSPARENT,
                    item.style.stroke ?: ColorRGBa.TRANSPARENT,
                    strokeWeight = item.style.strokeWeight ?: 0.0,
                    (item.style.strokeMode ?: StrokeMode.CENTER).mode
                )
                commands.add(cmd)
                cursorT += shape.advance.x * hscale * dt
            }
        }
    }
    slugDrawer.prepare(slugGlyphMap.slugMap, commands)
    slugDrawer.draw(drawer, slugGlyphMap.slugMap)
}
