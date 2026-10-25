package org.openrndr.extra.textwriter

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

enum class StrokeMode(val mode: Int) {
    CENTER(0),
    INNER(1),
    OUTER(2),
    ERODE(3),
}

/**
 * Represents a style configuration for text rendering.
 *
 * @property justify Determines whether the text should be justified.
 * @property lineHeightInEm Specifies the line height multiplier for the text.
 * @property horizontalAlign Defines the horizontal alignment of the text, typically in the range [0.0, 1.0].
 * @property verticalAlign Defines the vertical alignment of the text, typically in the range [0.0, 1.0].
 * @property direction The paragraph direction, either [Direction.LEFT_TO_RIGHT] or [Direction.RIGHT_TO_LEFT], or
 * null to determine it from the first strong character of every paragraph. Mixed-direction text is ordered using
 * [org.openrndr.draw.font.internal.BidiTextDriver].
 * @property language The language of the text as a language tag, for example `en`, `en-GB` or `nl`. It is passed to
 * the shaper and, when [hyphenate] is enabled, used to hyphenate the text automatically, see [Hyphenator.findPatternSet].
 * @property hyphenate Determines whether text with a [language] is hyphenated automatically. Words that contain soft
 * hyphens are not hyphenated automatically.
 */

@JvmRecord
data class TextStyle(
    val face: Face? = null,
    val sizeInEm: Double? = null,
    val justify: Boolean? = null,
    val lineHeightInEm: Double? = null,
    val textWidthFactor: Double? = null,
    val characterSpacingInEm: Double? = null,
    val baselineShiftInEm: Double? = null,
    val horizontalAlign: Double? = null,
    val verticalAlign: Double? = null,
    val fill: ColorRGBa? = null,
    val stroke: ColorRGBa? = null,
    val strokeWeight: Double? = null,
    val strokeMode: StrokeMode? = null,
    val features: List<ShapeFeature> = emptyList(),
    val direction: Direction? = null,
    val language: String? = null,
    val hyphenate: Boolean? = null,
) {
    fun cascade(other: TextStyle?): TextStyle {
        return if (other == null) this else copy(
            face = other.face ?: face,
            sizeInEm = other.sizeInEm ?: sizeInEm,
            justify = other.justify ?: justify,
            lineHeightInEm = other.lineHeightInEm ?: lineHeightInEm,
            textWidthFactor = other.textWidthFactor ?: textWidthFactor,
            characterSpacingInEm = other.characterSpacingInEm ?: characterSpacingInEm,
            baselineShiftInEm = other.baselineShiftInEm ?: baselineShiftInEm,
            horizontalAlign = other.horizontalAlign ?: horizontalAlign,
            verticalAlign = other.verticalAlign ?: verticalAlign,
            fill = other.fill ?: fill,
            stroke = other.stroke ?: stroke,
            strokeWeight = other.strokeWeight ?: strokeWeight,
            strokeMode = other.strokeMode ?: strokeMode,
            features = features + other.features,
            direction = other.direction ?: direction,
            language = other.language ?: language,
            hyphenate = other.hyphenate ?: hyphenate
        )
    }
    companion object {
        val Defaults = TextStyle(
            sizeInEm = 1.0,
            justify = false,
            lineHeightInEm = 1.0,
            textWidthFactor = 1.0,
            characterSpacingInEm = 0.0,
            baselineShiftInEm = 0.0,
            horizontalAlign = 0.0,
            verticalAlign = 0.0,
            fill = ColorRGBa.WHITE,
            strokeMode = StrokeMode.CENTER,
            direction = Direction.LEFT_TO_RIGHT,
            hyphenate = true
        )
    }
}

data class TextSpan(val text: String, val style: TextStyle?)


class SlugTextDrawer : TextDrawer {

    val shaper = TextShapingDriver.instance

    private val slugMap = SlugMap(
        colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
    )

    private val slugGlyphMap = SlugGlyphMap(slugMap)


    val commands = mutableListOf<SlugCommand>()

    override var style = TextStyle.Defaults


    override fun addText(spans: List<TextSpan>, boxes: List<Rectangle>) {
        addGlyphs(typesetText(spans, boxes, style, shaper).flatMap { it.glyphs })
    }

    override fun addGlyphs(glyphs: List<TypesetGlyph>) {
        // Make sure all glyphs are in the glyph map before slug indices are used, new glyphs are added in a single
        // batch so the slug map textures are written once
        slugMap.startBatch()
        try {
            for (glyph in glyphs) {
                slugGlyphMap.getSlugForGlyphIndex(glyph.face, glyph.glyphIndex)
            }
        } finally {
            slugMap.endBatch()
        }

        for (glyph in glyphs) {
            val glyphStyle = glyph.style
            commands.add(
                SlugCommand(
                    slugGlyphMap.getSlugForGlyphIndex(glyph.face, glyph.glyphIndex),
                    glyph.transform(),
                    glyphStyle.fill,
                    glyphStyle.stroke,
                    glyphStyle.strokeWeight ?: 0.0,
                    strokeMode = (glyphStyle.strokeMode ?: StrokeMode.CENTER).mode
                )
            )
        }
    }

    override fun addText(face: Face, text: String, position: Vector2) {
        addGlyphs(typesetLine(face, text, position, style, shaper))
    }

    override fun clear() {
        commands.clear()
    }

    override fun draw(drawer: Drawer) {
        val slugDrawer = Program.active?.drawer?.slugDrawer?: return

        slugDrawer.prepare(slugMap, commands)
        slugDrawer.draw(drawer, slugMap)
    }

}