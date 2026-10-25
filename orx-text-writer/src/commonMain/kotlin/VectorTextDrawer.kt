package org.openrndr.extra.textwriter

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape

/**
 * A glyph outline produced by [VectorTextDrawer].
 * @param shape the outline of the glyph, in drawer coordinates
 * @param style the fully cascaded style of the glyph
 */
class VectorGlyph(val shape: Shape, val style: TextStyle)

/**
 * A [TextDrawer] that produces glyph outlines as [Shape]s instead of drawing them as slugs. Use it to
 * export text as vectors, to process the outlines further or to draw text using the regular shape pipeline.
 *
 * [StrokeMode] is not supported by [draw], strokes are always centered.
 */
class VectorTextDrawer : TextDrawer {
    val shaper = TextShapingDriver.instance

    override var style = TextStyle.Defaults

    /**
     * The glyphs added so far, in the order they were added.
     */
    val glyphs = mutableListOf<VectorGlyph>()

    /**
     * The outlines of the glyphs added so far, one shape per glyph.
     */
    val shapes: List<Shape>
        get() = glyphs.map { it.shape }

    private val glyphShapes = mutableMapOf<Pair<Face, Int>, Shape>()

    private fun glyphShape(face: Face, glyphIndex: Int): Shape =
        glyphShapes.getOrPut(face to glyphIndex) { face.glyphForIndex(glyphIndex).shape() }

    override fun addText(spans: List<TextSpan>, boxes: List<Rectangle>) {
        addGlyphs(typesetText(spans, boxes, style, shaper).flatMap { it.glyphs })
    }

    override fun addText(face: Face, text: String, position: Vector2) {
        addGlyphs(typesetLine(face, text, position, style, shaper))
    }

    override fun addGlyphs(glyphs: List<TypesetGlyph>) {
        for (glyph in glyphs) {
            val shape = glyphShape(glyph.face, glyph.glyphIndex)
            if (shape.empty) continue
            this.glyphs.add(VectorGlyph(shape.transform(glyph.transform()), glyph.style))
        }
    }

    override fun clear() {
        glyphs.clear()
    }

    override fun draw(drawer: Drawer) {
        drawer.isolated {
            for (glyph in glyphs) {
                drawer.fill = glyph.style.fill
                drawer.stroke = glyph.style.stroke ?: ColorRGBa.TRANSPARENT
                drawer.strokeWeight = glyph.style.strokeWeight ?: 0.0
                drawer.shape(glyph.shape)
            }
        }
    }
}
