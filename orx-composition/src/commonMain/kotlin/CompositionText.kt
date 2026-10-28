package org.openrndr.extra.composition

import kotlin.math.atan2
import org.openrndr.math.asDegrees
import org.openrndr.math.transforms.transform
import org.openrndr.math.Vector3
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.typesetLine
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

private val logger = KotlinLogging.logger {}

/**
 * Draws the text of [TextNode]s with a [SlugTextDrawer]
 */
internal object CompositionText {
    // the slug drawer keeps its glyphs in textures, it is created once it is needed
    private val slugTextDrawer by lazy { SlugTextDrawer() }

    private val failedFonts = mutableSetOf<String>()

    // the slug drawer takes linear colors
    private fun Paint.color(opacity: Double): ColorRGBa? = when (this) {
        is Paint.RGB -> value.copy(alpha = 1.0).toLinear().opacify(opacity)
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

    /**
     * Draws the text of [node] starting at [pen], or at the position of the node when it has one. Text with a
     * contour is drawn along it, the x component of the pen and of the position is then the distance along the
     * contour.
     * @return the position after the text, where text that continues it starts
     */
    fun draw(drawer: Drawer, node: TextNode, pen: Vector2): Vector2 {
        val style = node.effectiveStyle
        val fontSize = (style.fontSize as? Length.Pixels)?.value ?: 16.0
        val file = CompositionFonts.resolve(style.fontFamily.value, style.fontWeight.value, style.fontStyle.value)
        if (file == null || file in failedFonts) {
            return pen
        }
        val face = try {
            loadFace(file).withSize(fontSize)
        } catch (e: Throwable) {
            failedFonts.add(file)
            logger.warn { "could not load font '$file': ${e.message}" }
            return pen
        }

        val opacity = opacity(node)
        val fill = style.fill.color(opacity * ((style.fillOpacity as? Numeric.Rational)?.value ?: 1.0))
        val stroke = style.stroke.color(opacity * ((style.strokeOpacity as? Numeric.Rational)?.value ?: 1.0))
        val textStyle = TextStyle.Defaults.copy(
            fill = fill,
            stroke = stroke,
            strokeWeight = if (stroke != null) (style.strokeWeight as? Length.Pixels)?.value else null
        )

        // spaces at the start and the end of a run separate it from the runs around it, the typesetter does not keep
        // them, their advances are added here
        val space = face.glyphForCharacter(' ').advanceWidth()
        val leading = node.text.takeWhile { it == ' ' }.length * space
        val trailing = if (node.text.isBlank()) 0.0 else node.text.takeLastWhile { it == ' ' }.length * space
        val glyphs = typesetLine(face, node.text.trim(' '), Vector2(leading, 0.0), textStyle)
        fun advance(glyph: TypesetGlyph) = face.glyphForIndex(glyph.glyphIndex).advanceWidth()
        // the advance of the text, used for its anchor and to continue the text after it
        val width = (glyphs.maxOfOrNull { it.position.x + advance(it) } ?: leading) + trailing
        val visible = fill != null || stroke != null

        val contour = node.contour
        if (contour == null) {
            val start = Vector2(node.x ?: pen.x, node.y ?: pen.y) + Vector2(node.dx, node.dy)
            val origin = start - Vector2(width * style.textAnchor.value, 0.0)
            if (visible) {
                slugTextDrawer.clear()
                slugTextDrawer.addGlyphs(glyphs.map { TypesetGlyph(it.face, it.glyphIndex, it.position + origin, it.style) })
                slugTextDrawer.draw(drawer)
            }
            return origin + Vector2(width, 0.0)
        } else {
            // the distance along the contour where the text starts, dy moves the text across the contour
            val start = (node.x ?: pen.x) + node.dx - width * style.textAnchor.value
            val arcLength = ArcLength(contour)
            val length = arcLength.length
            if (visible && length > 0.0) {
                // every glyph is placed with the middle of its advance on the contour, glyphs of which the middle is
                // not on the contour are not drawn
                val placed = glyphs.filter {
                    val middle = start + it.position.x + advance(it) / 2.0
                    middle in 0.0..length
                }
                slugTextDrawer.clear()
                slugTextDrawer.addGlyphs(placed) { glyph ->
                    val middle = start + glyph.position.x + advance(glyph) / 2.0
                    val t = arcLength.t(middle)
                    val direction = contour.direction(t)
                    transform {
                        translate(contour.position(t))
                        rotate(Vector3.UNIT_Z, atan2(direction.y, direction.x).asDegrees)
                        translate(-advance(glyph) / 2.0, glyph.position.y + node.dy)
                    }
                }
                slugTextDrawer.draw(drawer)
            }
            return Vector2(start + width, pen.y)
        }
    }
}

/**
 * Maps distances along a [contour] to its t values, from positions sampled at equal t steps
 */
private class ArcLength(private val contour: ShapeContour, samplesPerSegment: Int = 64) {
    private val sampleCount = (contour.segments.size * samplesPerSegment).coerceAtLeast(1)

    // the distance along the contour of every sample
    private val distances = DoubleArray(sampleCount + 1).also { distances ->
        var previous = contour.position(0.0)
        for (i in 1..sampleCount) {
            val position = contour.position(i.toDouble() / sampleCount)
            distances[i] = distances[i - 1] + position.distanceTo(previous)
            previous = position
        }
    }

    val length: Double get() = distances[sampleCount]

    /** the t value of the point at [distance] along the contour */
    fun t(distance: Double): Double {
        if (distance <= 0.0) return 0.0
        if (distance >= length) return 1.0
        var low = 0
        var high = sampleCount
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (distances[mid] < distance) low = mid else high = mid
        }
        val span = distances[high] - distances[low]
        val fraction = if (span > 0.0) (distance - distances[low]) / span else 0.0
        return (low + fraction) / sampleCount
    }
}
