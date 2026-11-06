package org.openrndr.extra.composition

import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * The bounds of what nodes draw, used to limit layers and the passes of filters to the pixels that a node covers.
 */

/** [rectangle] transformed by [m], the bounds of its transformed corners */
private fun transformed(rectangle: Rectangle, m: Matrix44): Rectangle =
    listOf(rectangle.corner, Vector2(rectangle.x + rectangle.width, rectangle.y),
        Vector2(rectangle.x, rectangle.y + rectangle.height), rectangle.corner + rectangle.dimensions)
        .map { (m * it.xy01).xy }.bounds

private fun Rectangle.offsetEdges(d: Double) = Rectangle(x - d, y - d, width + 2.0 * d, height + 2.0 * d)

private fun union(a: Rectangle, b: Rectangle) = listOf(a.corner, a.corner + a.dimensions, b.corner, b.corner + b.dimensions).bounds

/**
 * The bounds of what [node] draws in its own coordinates, before its own transform: its geometry widened by its
 * strokes, the cells of the glyphs of text, and the regions of filters. Null when they are not known, for example for
 * shapes with markers or strokes that do not scale; nothing is limited then.
 */
internal fun visualBounds(node: CompositionNode): Rectangle? {
    if (node.style.display == Display.None) return Rectangle(0.0, 0.0, 0.0, 0.0)
    val content: Rectangle = when (node) {
        is ShapeNode -> {
            if (node.shape.empty) return Rectangle(0.0, 0.0, 0.0, 0.0)
            val style = node.effectiveStyle
            if (style.markerStart.value != null || style.markerMid.value != null || style.markerEnd.value != null) return null
            if (style.vectorEffect == VectorEffect.NonScalingStroke) return null
            val bounds = node.shape.bounds
            if (style.stroke is Paint.None) bounds else {
                val weight = when (val w = style.strokeWeight) {
                    is Length.Pixels -> w.value
                    else -> return null
                }
                // miter joins reach up to the miter limit, square caps reach out diagonally
                val miter = if (style.lineJoin == LineJoin.Miter) max(1.0, (style.miterLimit as? Numeric.Rational)?.value ?: 4.0) else 1.0
                val cap = if (style.lineCap == LineCap.Square) sqrt(2.0) else 1.0
                bounds.offsetEdges(weight / 2.0 * max(miter, cap))
            }
        }
        is ImageNode -> Rectangle(node.x, node.y, node.width, node.height)
        is TextNode -> return null
        is GroupNode -> {
            if (node.textElement) {
                // glyphs reach beyond their cells, by their strokes and by overhangs of italics
                val measured = CompositionText.measure(node, Vector2.ZERO) ?: return Rectangle(0.0, 0.0, 0.0, 0.0)
                val style = node.effectiveStyle
                val size = (style.fontSize as? Length.Pixels)?.value ?: 16.0
                val stroke = if (style.stroke is Paint.None) 0.0 else ((style.strokeWeight as? Length.Pixels)?.value ?: 1.0)
                measured.offsetEdges(size * 0.25 + stroke)
            } else {
                var result: Rectangle? = null
                for (child in node.children) {
                    val b = visualBounds(child) ?: return null
                    if (b.width <= 0.0 && b.height <= 0.0) continue
                    val t = transformed(b, child.transform)
                    result = result?.let { union(it, t) } ?: t
                }
                result ?: return Rectangle(0.0, 0.0, 0.0, 0.0)
            }
        }
    }
    return filteredBounds(node, content)
}

/** the bounds of the result of the filters of [node], for [content] that the node draws, null when unbounded */
private fun filteredBounds(node: CompositionNode, content: Rectangle): Rectangle? {
    var bounds = content
    for (effect in node.style.filter.value) {
        bounds = effectBounds(effect, node, bounds) ?: return null
    }
    return bounds
}

/** the region of [effect] in user space for [node], null when it has no region or it is not known */
internal fun effectRegion(effect: FilterEffect, node: CompositionNode): Rectangle? {
    val region = effect.region ?: return null
    if (effect.regionUnits == PaintUnits.OBJECT) return region
    val b = localBounds(node) ?: return null
    return Rectangle(b.x + region.x * b.width, b.y + region.y * b.height, region.width * b.width, region.height * b.height)
}

/**
 * The bounds of the result of [effect] for [input], the bounds of what goes in: its region, or for effects without
 * a region, like the filter functions of CSS, the input widened by the reach of its blurs, offsets and morphology
 */
private fun effectBounds(effect: FilterEffect, node: CompositionNode, input: Rectangle): Rectangle? {
    if (effect.region != null) return effectRegion(effect, node)
    var reach = 0.0
    for (p in effect.primitives) {
        reach += when (p) {
            is FilterPrimitive.GaussianBlur -> 3.0 * max(p.stdDeviation.x, p.stdDeviation.y)
            is FilterPrimitive.DropShadow -> 3.0 * max(p.stdDeviation.x, p.stdDeviation.y) + max(abs(p.dx), abs(p.dy))
            is FilterPrimitive.Offset -> max(abs(p.dx), abs(p.dy))
            is FilterPrimitive.Morphology -> if (p.dilate) max(p.radius.x, p.radius.y) else 0.0
            is FilterPrimitive.DisplacementMap -> abs(p.scale) / 2.0
            // primitives that draw everywhere have no bounds
            is FilterPrimitive.Flood, is FilterPrimitive.Turbulence, is FilterPrimitive.Image, is FilterPrimitive.Tile -> return null
            else -> 0.0
        }
    }
    if (effect.primitiveUnits == PaintUnits.BOUNDS) {
        val b = localBounds(node) ?: return null
        reach *= max(b.width, b.height)
    }
    return input.offsetEdges(reach)
}

/**
 * The pixels of a target that a node covers: [uv], in texture coordinates, and [clip], the scissor rectangle in
 * the points of the target as [org.openrndr.draw.DrawStyle.clip] takes it
 */
internal class PixelBox(val uv: Rectangle, val clip: Rectangle) {
    /** the box as x0, y0, x1, y1 in texture coordinates */
    val uvBounds: Vector4 get() = Vector4(uv.x, uv.y, uv.x + uv.width, uv.y + uv.height)
}

/**
 * The pixels of a target of [width] by [height] points that [bounds] in user space cover, with a margin of two
 * pixels for anti-aliasing, mapped by [toClip] to clip space. Null when the bounds cover nothing.
 */
internal fun pixelBox(bounds: Rectangle, toClip: Matrix44, width: Double, height: Double, contentScale: Double): PixelBox? {
    val corners = listOf(bounds.corner, Vector2(bounds.x + bounds.width, bounds.y),
        Vector2(bounds.x, bounds.y + bounds.height), bounds.corner + bounds.dimensions)
        .map { (toClip * it.xy01).let { c -> Vector2(c.x / c.w, c.y / c.w) * 0.5 + Vector2(0.5, 0.5) } }
    val pw = width * contentScale
    val ph = height * contentScale
    // in pixels from the bottom left, whole pixels with a margin
    // a small tolerance keeps edges that fall on pixel boundaries there
    val x0 = max(0.0, floor(corners.minOf { it.x } * pw + 1E-6) - 2.0)
    val y0 = max(0.0, floor(corners.minOf { it.y } * ph + 1E-6) - 2.0)
    val x1 = min(pw, ceil(corners.maxOf { it.x } * pw - 1E-6) + 2.0)
    val y1 = min(ph, ceil(corners.maxOf { it.y } * ph - 1E-6) + 2.0)
    if (x1 <= x0 || y1 <= y0) return null
    val uv = Rectangle(x0 / pw, y0 / ph, (x1 - x0) / pw, (y1 - y0) / ph)
    val clip = Rectangle(x0 / contentScale, (ph - y1) / contentScale, (x1 - x0) / contentScale, (y1 - y0) / contentScale)
    return PixelBox(uv, clip)
}
