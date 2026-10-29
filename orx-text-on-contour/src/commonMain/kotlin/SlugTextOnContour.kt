package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.extra.textwriter.ContourBaseline
import org.openrndr.extra.textwriter.StrokeMode
import org.openrndr.extra.textwriter.layoutTextOnContour
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4

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
    val baseline = ContourBaseline(contour.contour, baselineOffset)
    val layout = layoutTextOnContour(spans, baseline, baseStyle, startDistance = startT * baseline.length)

    val commands = layout.glyphs.map { placed ->
        val style = placed.glyph.style
        SlugCommand(
            slugGlyphMap.getSlugForGlyphIndex(placed.glyph.face, placed.glyph.glyphIndex),
            placed.transform,
            style.fill ?: ColorRGBa.TRANSPARENT,
            style.stroke ?: ColorRGBa.TRANSPARENT,
            strokeWeight = style.strokeWeight ?: 0.0,
            (style.strokeMode ?: StrokeMode.CENTER).mode
        )
    }
    slugDrawer.prepare(slugGlyphMap.slugMap, commands)
    slugDrawer.draw(drawer, slugGlyphMap.slugMap)
}
