package org.openrndr.extra.composition

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.ContourBaseline
import org.openrndr.extra.textwriter.ContourOverflow
import org.openrndr.extra.textwriter.ContourTextLayout
import org.openrndr.extra.textwriter.GlyphPivot
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.layoutTextOnContour
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

/**
 * Draws the text of [TextNode]s with a [SlugTextDrawer]
 */
internal object CompositionText {
    // the slug drawer keeps its glyphs in textures, it is created once it is needed
    private val slugTextDrawer by lazy { SlugTextDrawer() }

    private val failedFonts = mutableSetOf<String>()

    private fun Paint.color(opacity: Double): ColorRGBa? = when (this) {
        is Paint.RGB -> value.copy(alpha = 1.0).opacify(opacity)
        else -> null
    }

    /** the opacity of [node] and its ancestors, the opacity of a group applies to its contents */
    private fun opacity(node: CompositionNode): Double {
        var opacity = 1.0
        var current: CompositionNode? = node
        while (current != null) {
            opacity *= (current.style.opacity as? Numeric.Rational)?.value ?: 1.0
            current = current.parent
        }
        return opacity
    }

    /** the face of the font of [style], null when there is no font or it cannot be loaded */
    private fun face(style: Style): Face? {
        val fontSize = (style.fontSize as? Length.Pixels)?.value ?: 16.0
        val file = CompositionFonts.resolve(style.fontFamily.value, style.fontWeight.value, style.fontStyle.value)
        if (file == null || file in failedFonts) {
            return null
        }
        return try {
            loadFace(file).withSize(fontSize)
        } catch (e: Throwable) {
            failedFonts.add(file)
            logger.warn { "could not load font '$file': ${e.message}" }
            null
        }
    }

    /** the fill and stroke of the text of [node] */
    private fun textStyle(node: TextNode, style: Style): TextStyle {
        val opacity = opacity(node)
        val fill = style.fill.color(opacity * ((style.fillOpacity as? Numeric.Rational)?.value ?: 1.0))
        val stroke = style.stroke.color(opacity * ((style.strokeOpacity as? Numeric.Rational)?.value ?: 1.0))
        return TextStyle.Defaults.copy(
            fill = fill,
            stroke = stroke,
            strokeWeight = if (stroke != null) (style.strokeWeight as? Length.Pixels)?.value else null
        )
    }

    /**
     * The text runs of [group] when they are all placed along the same contour, like the runs of an SVG textPath
     * element, null otherwise
     */
    fun contourRuns(group: GroupNode): List<TextNode>? {
        val runs = mutableListOf<TextNode>()
        fun collect(node: CompositionNode): Boolean = when (node) {
            is TextNode -> node.contour != null && (runs.isEmpty() || node.contour === runs.first().contour)
                .also { runs.add(node) }
            is GroupNode -> node.children.all(::collect)
            else -> false
        }
        return if (collect(group) && runs.isNotEmpty()) runs else null
    }

    /**
     * Draws the text of [node] starting at [pen], or at the position of the node when it has one. Text with a
     * contour is drawn along it, the x component of the pen and of the position is then the distance along the
     * contour.
     * @return the position after the text, where text that continues it starts
     */
    fun draw(drawer: Drawer, node: TextNode, pen: Vector2): Vector2 {
        if (node.contour != null) {
            return drawOnContour(drawer, listOf(node), pen)
        }
        val style = node.effectiveStyle
        val face = face(style) ?: return pen
        val textStyle = textStyle(node, style)

        // spaces at the start and the end of a run separate it from the runs around it, the typesetter does not keep
        // them, their advances are added here
        val space = face.glyphForCharacter(' ').advanceWidth()
        val leading = node.text.takeWhile { it == ' ' }.length * space
        val trailing = if (node.text.isBlank()) 0.0 else node.text.takeLastWhile { it == ' ' }.length * space
        val glyphs = typesetLine(face, node.text.trim(' '), Vector2(leading, 0.0), textStyle)
        fun advance(glyph: TypesetGlyph) = face.glyphForIndex(glyph.glyphIndex).advanceWidth()
        // the advance of the text, used for its anchor and to continue the text after it
        val width = (glyphs.maxOfOrNull { it.position.x + advance(it) } ?: leading) + trailing

        val start = Vector2(node.x ?: pen.x, node.y ?: pen.y) + Vector2(node.dx, node.dy)
        val origin = start - Vector2(width * style.textAnchor.value, 0.0)
        if (textStyle.fill != null || textStyle.stroke != null) {
            slugTextDrawer.clear()
            slugTextDrawer.addGlyphs(glyphs.map { TypesetGlyph(it.face, it.glyphIndex, it.position + origin, it.style) })
            slugTextDrawer.draw(drawer)
        }
        return origin + Vector2(width, 0.0)
    }

    /**
     * Draws [runs] that share a contour along it, like SVG draws the text of a textPath element: the runs are shaped
     * as a single line, the middle of every glyph is placed on the contour and glyphs beyond its ends are not drawn.
     * The first run determines where the text starts and its anchor. The dx of every run moves the text from that run
     * on along the contour, its dy moves it across the contour.
     * @return the position after the text, its x component is the distance along the contour
     */
    fun drawOnContour(drawer: Drawer, runs: List<TextNode>, pen: Vector2): Vector2 {
        val first = runs.first()
        val contour = first.contour ?: return pen
        val spans = mutableListOf<TextSpan>()
        val shifts = mutableListOf<Vector2>()
        for (run in runs) {
            val style = run.effectiveStyle
            val face = face(style) ?: continue
            spans.add(TextSpan(run.text, textStyle(run, style).copy(face = face)))
            shifts.add(Vector2(run.dx, run.dy))
        }
        if (spans.isEmpty()) {
            return pen
        }
        val layout = layoutTextOnContour(
            spans,
            ContourBaseline(contour, measureOnBaseline = false),
            // the fill and stroke of every span are complete, a span without a fill is not filled
            style = TextStyle.Defaults.copy(fill = null),
            startDistance = first.x ?: pen.x,
            anchor = first.effectiveStyle.textAnchor.value,
            overflow = ContourOverflow.HIDE,
            pivot = GlyphPivot.MIDDLE,
            shifts = shifts
        )
        val visible = layout.glyphs.filter { it.glyph.style.fill != null || it.glyph.style.stroke != null }
        if (visible.isNotEmpty()) {
            slugTextDrawer.clear()
            slugTextDrawer.addGlyphs(ContourTextLayout(visible, layout.start, layout.advance))
            slugTextDrawer.draw(drawer)
        }
        return Vector2(layout.end, pen.y)
    }
}
