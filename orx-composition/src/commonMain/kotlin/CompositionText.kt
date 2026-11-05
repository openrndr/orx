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
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * Draws the text of [TextNode]s with a [SlugTextDrawer]
 */
internal object CompositionText {
    // the slug drawer keeps its glyphs in textures, it is created once it is needed
    private val slugTextDrawer by lazy { SlugTextDrawer() }

    private val failedFonts = mutableSetOf<String>()

    // text is drawn in a single color, gradients and other paints are drawn in the color that stands in for them
    private fun Paint.color(opacity: Double, style: Style): ColorRGBa? = when (this) {
        is Paint.RGB -> value.opacify(opacity)
        is Paint.Server -> value?.copy(alpha = 1.0)?.opacify(opacity)
        is Paint.CurrentColor -> currentColor(style)?.opacify(opacity)
        else -> null
    }

    /** the opacity of [node], the opacity of the groups it is in is applied by their layers */
    private fun opacity(node: CompositionNode): Double = (node.style.opacity as? Numeric.Rational)?.value ?: 1.0

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

    /** the paint servers of the fill and stroke of [style], null when neither is one */
    private fun servers(style: Style): Pair<Paint.Server?, Paint.Server?>? {
        val fill = style.fill as? Paint.Server
        val stroke = style.stroke as? Paint.Server
        return if (fill == null && stroke == null) null else Pair(fill, stroke)
    }

    /**
     * The fill and stroke of the text of [node]. Paints that are drawn with a paint style are white, the paint style
     * replaces the color and keeps the alpha.
     */
    private fun textStyle(node: TextNode, style: Style): TextStyle {
        val opacity = opacity(node)
        fun color(paint: Paint, paintOpacity: Double): ColorRGBa? = when (paint) {
            is Paint.Server -> ColorRGBa.WHITE.opacify(paintOpacity)
            else -> paint.color(paintOpacity, style)
        }
        val fill = color(style.fill, opacity * ((style.fillOpacity as? Numeric.Rational)?.value ?: 1.0))
        val stroke = color(style.stroke, opacity * ((style.strokeOpacity as? Numeric.Rational)?.value ?: 1.0))
        val fontSize = fontSize(style)
        return TextStyle.Defaults.copy(
            fill = fill,
            stroke = stroke,
            strokeWeight = if (stroke != null) (style.strokeWeight as? Length.Pixels)?.value else null,
            characterSpacingInEm = pixels(style.letterSpacing, fontSize) / fontSize,
            wordSpacingInEm = pixels(style.wordSpacing, fontSize) / fontSize
        )
    }

    private fun fontSize(style: Style) = (style.fontSize as? Length.Pixels)?.value ?: 16.0

    /** [length] in pixels, a percentage is one of [fontSize] */
    private fun pixels(length: Length, fontSize: Double) = when (length) {
        is Length.Pixels -> length.value
        is Length.Percent -> fontSize * length.value / 100.0
    }

    /**
     * How far the alphabetic baseline of [node] is below its position: the offset of its dominant baseline minus the
     * baseline shifts of the node and of the groups it is in
     */
    private fun baselineOffset(node: TextNode, style: Style, face: Face): Double {
        val dominant = style.dominantBaseline.offset(abs(face.ascent), abs(face.descent), face.xHeight)
        var shift = 0.0
        var current: CompositionNode? = node
        while (current != null) {
            val own = current.style
            if (own.properties[AttributeOrPropertyKey.BASELINE_SHIFT] != null) {
                val currentStyle = current.effectiveStyle
                shift += when (val baselineShift = own.baselineShift) {
                    is BaselineShift.Value -> pixels(baselineShift.value, fontSize(currentStyle))
                    else -> {
                        val currentFace = face(currentStyle) ?: face
                        val half = (abs(currentFace.ascent) + abs(currentFace.descent)) / 2.0
                        if (baselineShift is BaselineShift.Sub) -half else half
                    }
                }
            }
            current = current.parent
        }
        return dominant - shift
    }

    /**
     * Sets the paint style for text with [style] on [drawer], with paints over the bounds of the text mapped to
     * [bounds]. Paints that can not be drawn, over bounds that are not known or are empty, are left out.
     * @return false when nothing of the text is drawn
     */
    private fun applyPaints(drawer: Drawer, style: Style, bounds: Rectangle?): Boolean {
        val (fillServer, strokeServer) = servers(style) ?: return true
        val emptyBounds = Rectangle(0.0, 0.0, 0.0, 0.0)
        val fillTransform = fillServer?.let { drawer.preparePaint(it, bounds ?: emptyBounds) }
        val strokeTransform = strokeServer?.let { drawer.preparePaint(it, bounds ?: emptyBounds) }
        val fill = if (fillTransform != null) fillServer else null
        val stroke = if (strokeTransform != null) strokeServer else null
        if (fill == null && stroke == null) {
            // the paints of a run with a server have no color to fall back on
            return style.fill !is Paint.Server && style.stroke !is Paint.Server
        }
        val paint = paintStyle(fill, stroke)
        fillTransform?.let { paint.fillTransform = it }
        strokeTransform?.let { paint.strokeTransform = it }
        drawer.shadeStyle = paint
        return true
    }

    /**
     * The runs of [group] when it holds only text, in order, null otherwise
     */
    fun textRuns(group: GroupNode): List<TextNode>? = group.children.flatMap {
        when (it) {
            is TextNode -> listOf(it)
            is GroupNode -> textRuns(it) ?: return null
            else -> return null
        }
    }

    /** true when the fill or stroke of a run of [runs] is a paint that is drawn with a paint style */
    fun hasPaints(runs: List<TextNode>): Boolean = runs.any { servers(it.effectiveStyle) != null }

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
     * The glyphs of a run, placed, and the position after the run. [origin] is the start of the baseline of the run,
     * [rotate] the angle every glyph is turned by.
     */
    private class RunLayout(val glyphs: List<TypesetGlyph>, val end: Vector2, val origin: Vector2, val rotate: Double) {
        /** the transform of the outline of [glyph], turned around its origin */
        fun outline(glyph: TypesetGlyph): Matrix44 = if (rotate == 0.0) glyph.transform() else
            buildTransform { translate(glyph.position); rotate(rotate); translate(-glyph.position) } * glyph.transform()
    }

    /**
     * Lays out the text of [node] starting at [pen], or at the position of the node when it has one
     */
    private fun layoutRun(node: TextNode, pen: Vector2): RunLayout? {
        val style = node.effectiveStyle
        val face = face(style) ?: return null
        val textStyle = textStyle(node, style)

        // spaces at the start and the end of a run separate it from the runs around it, the typesetter does not keep
        // them, their advances are added here
        val fontSize = fontSize(style)
        val letterSpacing = pixels(style.letterSpacing, fontSize)
        val space = face.glyphForCharacter(' ').advanceWidth() + letterSpacing + pixels(style.wordSpacing, fontSize)
        val leading = node.text.takeWhile { it == ' ' }.length * space
        val trailing = if (node.text.isBlank()) 0.0 else node.text.takeLastWhile { it == ' ' }.length * space
        val glyphs = typesetLine(face, node.text.trim(' '), Vector2(leading, 0.0), textStyle)
        fun advance(glyph: TypesetGlyph) = face.glyphForIndex(glyph.glyphIndex).advanceWidth() + letterSpacing
        // the advance of the text, used for its anchor and to continue the text after it
        val width = (glyphs.maxOfOrNull { it.position.x + advance(it) } ?: leading) + trailing

        val start = Vector2(node.x ?: pen.x, node.y ?: pen.y) + Vector2(node.dx, node.dy)
        val origin = start - Vector2(width * style.textAnchor.value, 0.0)
        // the glyphs are on the alphabetic baseline, text continues on the baseline of the position
        val baseline = Vector2(0.0, baselineOffset(node, style, face))
        return RunLayout(
            glyphs.map { TypesetGlyph(it.face, it.glyphIndex, it.position + origin + baseline, it.style) },
            origin + Vector2(width, 0.0),
            origin + baseline,
            node.rotate
        )
    }

    /**
     * Draws the text of [node] starting at [pen], or at the position of the node when it has one. Text with a
     * contour is drawn along it, the x component of the pen and of the position is then the distance along the
     * contour. Paints over the bounds of the text are mapped to [textBounds], the bounds of the text element.
     * @return the position after the text, where text that continues it starts
     */
    fun draw(drawer: Drawer, node: TextNode, pen: Vector2, textBounds: Rectangle? = null): Vector2 {
        if (node.contour != null) {
            return drawOnContour(drawer, listOf(node), pen, textBounds)
        }
        val layout = layoutRun(node, pen) ?: return pen
        val style = node.effectiveStyle
        val visible = layout.glyphs.filter { it.style.fill != null || it.style.stroke != null }
        // a run that is not part of a text element is a text of its own
        val bounds = textBounds ?: layout.glyphs.flatMap { cell(it, layout.outline(it)) }.takeIf { it.isNotEmpty() }?.bounds
        // hidden text is laid out, text after it continues after it, but it is not drawn
        if (visible.isNotEmpty() && style.visibility.value && applyPaints(drawer, style, bounds)) {
            slugTextDrawer.clear()
            if (layout.rotate == 0.0) slugTextDrawer.addGlyphs(visible)
            else slugTextDrawer.addGlyphs(visible) { layout.outline(it) }
            drawer.applyClip()
            slugTextDrawer.draw(drawer)
            drawDecorations(drawer, node, style, layout)
        }
        return layout.end
    }

    /**
     * Draws the lines of the text decoration of [style] along the run of [node] laid out as [layout], in the fill
     * and stroke of the text
     */
    private fun drawDecorations(drawer: Drawer, node: TextNode, style: Style, layout: RunLayout) {
        val decoration = style.textDecoration
        if (!decoration.underline && !decoration.overline && !decoration.lineThrough) return
        val face = face(style) ?: return
        val textStyle = textStyle(node, style)
        // the underline and the line through are where the face puts them, the overline is on top of the ascent
        val metrics = face.decorationMetrics
        val ascent = abs(face.ascent)
        val lines = listOfNotNull(
            Pair(-metrics.underlinePosition, metrics.underlineThickness).takeIf { decoration.underline },
            Pair(-ascent - metrics.underlineThickness / 2.0, metrics.underlineThickness).takeIf { decoration.overline },
            Pair(-metrics.strikeoutPosition, metrics.strikeoutThickness).takeIf { decoration.lineThrough }
        )
        val start = layout.origin.x
        val end = layout.end.x
        if (end <= start) return
        drawer.pushStyle()
        drawer.fill = textStyle.fill
        drawer.stroke = textStyle.stroke
        drawer.strokeWeight = textStyle.strokeWeight ?: 1.0
        val rectangles = lines.map { (y, thickness) ->
            Rectangle(start, layout.origin.y + y - thickness / 2.0, end - start, thickness)
        }
        drawer.applyClip()
        drawer.rectangles(rectangles)
        drawer.popStyle()
    }

    /**
     * Lays out [runs] that share a contour along it, like SVG places the text of a textPath element. Runs that are
     * not [shown] are laid out without fill and stroke.
     */
    private fun layoutOnContour(
        runs: List<TextNode>,
        pen: Vector2,
        shown: (TextNode) -> Boolean = { true }
    ): ContourTextLayout? {
        val first = runs.first()
        val contour = first.contour ?: return null
        val spans = mutableListOf<TextSpan>()
        val shifts = mutableListOf<Vector2>()
        // shifts accumulate, the baseline of every run is shifted by the difference with the previous run
        var previousBaseline = 0.0
        for (run in runs) {
            val style = run.effectiveStyle
            val face = face(style) ?: continue
            val textStyle = textStyle(run, style).copy(face = face)
            spans.add(TextSpan(run.text, if (shown(run)) textStyle else textStyle.copy(fill = null, stroke = null)))
            val baseline = baselineOffset(run, style, face)
            shifts.add(Vector2(run.dx, run.dy + baseline - previousBaseline))
            previousBaseline = baseline
        }
        if (spans.isEmpty()) {
            return null
        }
        return layoutTextOnContour(
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
    }

    /**
     * Draws [runs] that share a contour along it, like SVG draws the text of a textPath element: the runs are shaped
     * as a single line, the middle of every glyph is placed on the contour and glyphs beyond its ends are not drawn.
     * The first run determines where the text starts and its anchor. The dx of every run moves the text from that run
     * on along the contour, its dy moves it across the contour. Runs with different paints are drawn separately,
     * paints over the bounds of the text are mapped to [textBounds].
     * @return the position after the text, its x component is the distance along the contour
     */
    fun drawOnContour(drawer: Drawer, runs: List<TextNode>, pen: Vector2, textBounds: Rectangle? = null): Vector2 {
        val all = layoutOnContour(runs, pen) ?: return pen
        // runs that are not part of a text element are a text of their own
        val bounds = textBounds ?: all.glyphs.flatMap { cell(it.glyph, it.transform) }.takeIf { it.isNotEmpty() }?.bounds
        // the runs are drawn in groups that share their paints
        val groups = runs.groupBy { servers(it.effectiveStyle) }
        fun shown(run: TextNode) = run.effectiveStyle.visibility.value
        val anyHidden = runs.any { !shown(it) }
        for ((paints, groupRuns) in groups) {
            val layout = if (groups.size == 1 && !anyHidden) all else
                layoutOnContour(runs, pen) { it in groupRuns && shown(it) } ?: continue
            val visible = layout.glyphs.filter { it.glyph.style.fill != null || it.glyph.style.stroke != null }
            if (visible.isEmpty()) continue
            drawer.pushStyle()
            val drawn = paints == null || applyPaints(drawer, groupRuns.first().effectiveStyle, bounds)
            if (drawn) {
                slugTextDrawer.clear()
                slugTextDrawer.addGlyphs(ContourTextLayout(visible, layout.start, layout.advance))
                drawer.applyClip()
                slugTextDrawer.draw(drawer)
            }
            drawer.popStyle()
        }
        return Vector2(all.end, pen.y)
    }

    /**
     * The corners of the glyph cell of [glyph], its advance by the ascent and descent of its face, in the coordinates
     * of the text. [outline] is the transform of the outline of the glyph when it is not placed by its position, as
     * for text along a contour.
     */
    private fun cell(glyph: TypesetGlyph, outline: Matrix44? = null): List<Vector2> {
        val face = glyph.face
        val advance = face.glyphForIndex(glyph.glyphIndex).advanceWidth()
        val p = glyph.position
        val top = p.y - abs(face.ascent)
        val bottom = p.y + abs(face.descent)
        val corners = listOf(Vector2(p.x, top), Vector2(p.x + advance, top), Vector2(p.x + advance, bottom), Vector2(p.x, bottom))
        if (outline == null) return corners
        val m = outline * glyph.transform().inversed
        return corners.map { (m * it.xy01).xy }
    }

    /**
     * The bounds of the text of [group], the union of the cells of its glyphs, like SVG's bounding box of a text
     * element, laid out starting at [pen]. Null when it has no glyphs.
     */
    fun measure(group: GroupNode, pen: Vector2): Rectangle? {
        val points = mutableListOf<Vector2>()
        var current = pen
        fun visit(node: CompositionNode) {
            when (node) {
                is TextNode -> if (node.contour != null) {
                    layoutOnContour(listOf(node), current)?.let { layout ->
                        layout.glyphs.forEach { points.addAll(cell(it.glyph, it.transform)) }
                        current = Vector2(layout.end, current.y)
                    }
                } else {
                    layoutRun(node, current)?.let { layout ->
                        layout.glyphs.forEach { points.addAll(if (layout.rotate == 0.0) cell(it) else cell(it, layout.outline(it))) }
                        current = layout.end
                    }
                }

                is GroupNode -> {
                    val contourRuns = contourRuns(node)
                    if (contourRuns != null) {
                        layoutOnContour(contourRuns, current)?.let { layout ->
                            layout.glyphs.forEach { points.addAll(cell(it.glyph, it.transform)) }
                            current = Vector2(layout.end, current.y)
                        }
                    } else {
                        node.children.forEach(::visit)
                    }
                }

                else -> {}
            }
        }
        visit(group)
        return if (points.isEmpty()) null else points.bounds
    }
}
