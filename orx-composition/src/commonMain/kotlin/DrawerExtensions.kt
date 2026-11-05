package org.openrndr.extra.composition

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.math.Matrix55
import org.openrndr.draw.Drawer
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.shadestyles.presets.PaintLayer
import org.openrndr.extra.shadestyles.presets.PaintStyle
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.shadestyles.presets.boundsTransform
import org.openrndr.math.Matrix44
import org.openrndr.color.ColorRGBa

/** the paint styles of pairs of fill and stroke paints, gradients with the same values share a paint style */
private val paintStyles = LinkedHashMap<Pair<Paint.Server?, Paint.Server?>, PaintStyle>()

/**
 * The paint style that draws [fill] and [stroke]. Paints are read in object units, the transforms of a shape are set
 * by [paintTransform] before it is drawn.
 */
internal fun paintStyle(fill: Paint.Server?, stroke: Paint.Server?): PaintStyle {
    val key = Pair(fill, stroke)
    paintStyles[key]?.let { return it }
    if (paintStyles.size >= 256) {
        paintStyles.remove(paintStyles.keys.first())
    }
    fun layer(paint: Paint.Server?) = paint?.let { PaintLayer(it.source(), PaintUnits.OBJECT) }
    return PaintStyle(layer(fill), layer(stroke)).also { paintStyles[key] = it }
}

/**
 * The transform of [paint] in object units for a shape with [bounds]: paints over the bounds of the shape are mapped
 * to them. Null when the bounds have no width or height, then the paint is not drawn, as in SVG.
 */
internal fun paintTransform(paint: Paint.Server, bounds: org.openrndr.shape.Rectangle): Matrix44? = when (paint.units) {
    PaintUnits.OBJECT -> paint.transform
    PaintUnits.BOUNDS -> if (bounds.width <= 0.0 || bounds.height <= 0.0) null else boundsTransform(bounds) * paint.transform
}

/** true when [style] draws anything: it has code */
private fun hasCode(style: ShadeStyle): Boolean =
    !style.fragmentTransform.isNullOrBlank() || !style.vertexTransform.isNullOrBlank() ||
            !style.fragmentPreamble.isNullOrBlank() || !style.vertexPreamble.isNullOrBlank()


/**
 * Renders a vector `Composition` onto the `Drawer`. This method applies transformations, styles,
 * and renders the hierarchy of nodes from the given `Composition` object.
 *
 * @param composition The vector composition containing the root node and associated dimensions.
 *                    It includes the styling and viewport transformation details necessary for rendering.
 */
fun Drawer.composition(composition: Composition) {
    pushModel()
    pushStyle()

    // viewBox transformation
    model *= composition.calculateViewportTransform()

    // where text without a position continues
    var pen = Vector2.ZERO

    // the bounds of the text element that is drawn, when its paints need them
    var textBounds: Rectangle? = null

    fun node(compositionNode: CompositionNode) {
        pushModel()
        pushStyle()
        model *= compositionNode.style.transform.value

        shadeStyle = (compositionNode.style.shadeStyle as Shade.Value).value

        when (compositionNode) {
            is ShapeNode -> {
                // fills and strokes are inherited from the groups the shape is in
                val style = compositionNode.effectiveStyle

                style.stroke.let {
                    stroke = when (it) {
                        is Paint.RGB -> it.value.copy(alpha = 1.0)
                        is Paint.Server -> ColorRGBa.WHITE
                        Paint.None -> null
                        Paint.CurrentColor -> null
                    }
                }
                style.strokeOpacity.let {
                    stroke = when (it) {
                        is Numeric.Rational -> stroke?.opacify(it.value)
                    }
                }
                style.strokeWeight.let {
                    strokeWeight = when (it) {
                        is Length.Pixels -> it.value
                        is Length.Percent -> composition.normalizedDiagonalLength() * it.value / 100.0
                    }
                }
                style.miterLimit.let {
                    miterLimit = when (it) {
                        is Numeric.Rational -> it.value
                    }
                }
                style.lineCap.let {
                    lineCap = it.value
                }
                style.lineJoin.let {
                    lineJoin = it.value
                }
                style.fill.let {
                    fill = when (it) {
                        is Paint.RGB -> it.value.copy(alpha = 1.0)
                        is Paint.Server -> ColorRGBa.WHITE
                        is Paint.None -> null
                        is Paint.CurrentColor -> null
                    }
                }
                // gradients and other paints are drawn by a paint style, which takes the alpha of the fill and stroke
                val fillServer = style.fill as? Paint.Server
                val strokeServer = style.stroke as? Paint.Server
                if (fillServer != null || strokeServer != null) {
                    val paint = paintStyle(fillServer, strokeServer)
                    val bounds = compositionNode.shape.bounds
                    if (fillServer != null) {
                        val transform = paintTransform(fillServer, bounds)
                        if (transform == null) fill = null else paint.fillTransform = transform
                    }
                    if (strokeServer != null) {
                        val transform = paintTransform(strokeServer, bounds)
                        if (transform == null) stroke = null else paint.strokeTransform = transform
                    }
                    val own = shadeStyle
                    shadeStyle = if (own != null && hasCode(own)) own + paint else paint
                }
                style.fillOpacity.let {
                    fill = when (it) {
                        is Numeric.Rational -> fill?.opacify(it.value)
                    }
                }
                // the opacity of the shape and of the groups it is in
                val opacity = generateSequence(compositionNode as CompositionNode) { it.parent }
                    .fold(1.0) { acc, node -> acc * ((node.style.opacity as? Numeric.Rational)?.value ?: 1.0) }
                stroke = stroke?.opacify(opacity)
                fill = fill?.opacify(opacity)
                shape(compositionNode.shape)
            }
            is ImageNode -> {
                val image = compositionNode.image
                val source = compositionNode.source ?: Rectangle(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
                val target = Rectangle(compositionNode.x, compositionNode.y, compositionNode.width, compositionNode.height)
                (compositionNode.style.opacity as? Numeric.Rational)?.let {
                    if (it.value < 1.0) {
                        // scales the alpha of the image
                        drawStyle.colorMatrix = Matrix55.IDENTITY.copy(c3r3 = it.value)
                    }
                }
                image(image, source, target)
            }
            is TextNode -> {
                pen = CompositionText.draw(this, compositionNode, pen, textBounds)
            }
            is GroupNode -> {
                // paints over the bounds of text are mapped to the bounds of the whole text element
                val outerBounds = textBounds
                if (compositionNode.textElement) {
                    val runs = CompositionText.textRuns(compositionNode)
                    if (runs != null && CompositionText.hasPaints(runs)) {
                        textBounds = CompositionText.measure(compositionNode, pen)
                    }
                }
                // text along a contour is shaped and placed as a whole
                val contourRuns = CompositionText.contourRuns(compositionNode)
                if (contourRuns != null) {
                    pen = CompositionText.drawOnContour(this, contourRuns, pen, textBounds)
                } else {
                    compositionNode.children.forEach { node(it) }
                }
                textBounds = outerBounds
            }
        }
        popModel()
        popStyle()
    }
    node(composition.root)
    popModel()
    popStyle()
}