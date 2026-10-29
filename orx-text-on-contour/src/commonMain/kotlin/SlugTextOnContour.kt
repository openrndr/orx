package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
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
 * Draws [spans] on a single line along [contour] with [boundaryDrawer], for example a
 * [org.openrndr.draw.slug.SlugDrawer] or the boundary drawer of a [Drawer]. The boundaries of the glyphs are kept in
 * [glyphMap]. Text that is longer than the contour wraps around a closed contour.
 *
 * @param baseStyle the style that is cascaded into the style of every span
 * @param startT the normalized position on the contour at which the text starts
 * @param baselineOffset the distance between the contour and the baseline of the text, positive distances move the
 * text towards its ascenders. Glyph spacing is measured on the offset baseline.
 */
fun slugTextOnContour(
    drawer: Drawer,
    boundaryDrawer: BoundaryDrawer,
    glyphMap: BoundaryGlyphMap,
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
        BoundaryCommand(
            glyphMap.getBoundaryForGlyphIndex(placed.glyph.face, placed.glyph.glyphIndex),
            placed.transform,
            style.fill ?: ColorRGBa.TRANSPARENT,
            style.stroke ?: ColorRGBa.TRANSPARENT,
            strokeWeight = style.strokeWeight ?: 0.0,
            (style.strokeMode ?: StrokeMode.CENTER).mode
        )
    }
    boundaryDrawer.prepare(glyphMap.map, commands)
    boundaryDrawer.draw(drawer, glyphMap.map)
}
