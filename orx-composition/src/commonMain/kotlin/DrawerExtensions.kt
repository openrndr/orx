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
import org.openrndr.draw.isolated
import kotlin.math.atan2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.Vector3
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Shape
import org.openrndr.shape.bounds
import org.openrndr.math.IntVector2
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.WrapMode
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
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
 * Prepares [paint] for a shape with [bounds] and returns its transform in object units: paints over the bounds of the
 * shape are mapped to them, the tile of a pattern is drawn to a texture. Null when the paint is not drawn: when it is
 * over bounds without width or height, or a pattern with an empty tile, as in SVG.
 */
internal fun Drawer.preparePaint(paint: Paint.Server, bounds: Rectangle): Matrix44? {
    val emptyBounds = bounds.width <= 0.0 || bounds.height <= 0.0
    return when (paint) {
        is Paint.Pattern -> {
            if (paint.units == PaintUnits.BOUNDS && emptyBounds) return null
            // the tile in the coordinates of the shape
            val tile = when (paint.units) {
                PaintUnits.BOUNDS -> Rectangle(
                    bounds.x + paint.tile.x * bounds.width, bounds.y + paint.tile.y * bounds.height,
                    paint.tile.width * bounds.width, paint.tile.height * bounds.height
                )
                PaintUnits.OBJECT -> paint.tile
            }
            if (tile.width <= 0.0 || tile.height <= 0.0) return null
            val texture = patternTile(paint, tile, bounds) ?: return null
            paint.texturePaint(texture)
            // the tile texture is stored bottom up, the top of the tile is at texture coordinate 1
            paint.transform * boundsTransform(tile) * Matrix44.translate(0.0, 1.0, 0.0) * Matrix44.scale(1.0, -1.0, 1.0)
        }
        else -> when (paint.units) {
            PaintUnits.OBJECT -> paint.transform
            PaintUnits.BOUNDS -> if (emptyBounds) null else boundsTransform(bounds) * paint.transform
        }
    }
}

/** the textures of the tiles of patterns, by pattern, size in pixels and placement of the content */
private val patternTiles = LinkedHashMap<Triple<Paint.Pattern, IntVector2, Matrix44>, ColorBuffer>()

/**
 * The texture of the tile of [pattern] with the size of [tile], at the scale it is drawn at, which wraps around to
 * repeat the tile. [bounds] are the bounds of the shape, which content in bounds units is relative to.
 */
private fun Drawer.patternTile(pattern: Paint.Pattern, tile: Rectangle, bounds: Rectangle): ColorBuffer? {
    // the size in pixels: the size of the tile scaled like the pattern is drawn
    val m = model * pattern.transform
    // the scale is rounded up to steps of a quarter octave, so tiles are not drawn again for every small change of
    // scale, such as while zooming
    val exactScale = sqrt(abs(m.c0r0 * m.c1r1 - m.c0r1 * m.c1r0)).coerceAtLeast(1E-6)
    val scale = 2.0.pow(ceil(log2(exactScale) * 4.0) / 4.0)
    val contentScale = RenderTarget.active.contentScale
    val size = IntVector2(
        ceil(tile.width * scale * contentScale).toInt().coerceIn(1, 2048),
        ceil(tile.height * scale * contentScale).toInt().coerceIn(1, 2048)
    )
    // places the content in the tile, in the units of the tile
    val content = when {
        pattern.viewBox != null -> viewportTransform(pattern.viewBox, Rectangle(0.0, 0.0, tile.width, tile.height), pattern.aspectRatio)
        pattern.contentUnits == PaintUnits.BOUNDS -> Matrix44.scale(bounds.width, bounds.height, 1.0)
        else -> Matrix44.IDENTITY
    }
    val key = Triple(pattern, size, content)
    patternTiles[key]?.let { return it }
    if (patternTiles.size >= 64) {
        patternTiles.remove(patternTiles.keys.first())?.destroy()
    }

    val target = renderTarget(size.x, size.y, contentScale = 1.0) {
        colorBuffer()
        depthBuffer()
    }
    val bounds = Rectangle(0.0, 0.0, size.x.toDouble(), size.y.toDouble())
    isolatedWithTarget(target) {
        ortho(target)
        clear(ColorRGBa.TRANSPARENT)
        // the tile is drawn with its neighbours, every copy clipped to its tile and a pixel around it: shapes that end at
        // the edge of the tile are anti-aliased by the shapes of the neighbouring tiles, so the tiles repeat without
        // seams, while content beyond the edge of a tile is clipped, as in SVG
        val composition = Composition(pattern.content)
        for (j in -1..1) {
            for (i in -1..1) {
                val x0 = max(i * size.x - 1.0, 0.0)
                val y0 = max(j * size.y - 1.0, 0.0)
                val x1 = min((i + 1) * size.x + 1.0, bounds.width)
                val y1 = min((j + 1) * size.y + 1.0, bounds.height)
                if (x1 <= x0 || y1 <= y0) continue
                drawStyle.clip = Rectangle(x0, y0, x1 - x0, y1 - y0)
                model = Matrix44.translate(i * size.x.toDouble(), j * size.y.toDouble(), 0.0) *
                        Matrix44.scale(size.x / tile.width, size.y / tile.height, 1.0) * content
                composition(composition)
            }
        }
        drawStyle.clip = null
    }
    val texture = target.colorBuffer(0)
    target.detachColorAttachments()
    target.destroy()
    texture.wrapU = WrapMode.REPEAT
    texture.wrapV = WrapMode.REPEAT
    patternTiles[key] = texture
    return texture
}

/**
 * The dashes of a shape: [contours] of the dashes, and the positions and directions of [points], dashes of length
 * zero, which only have caps
 */
internal class Dashes(val contours: List<ShapeContour>, val points: List<Pair<Vector2, Vector2>>)

/** the dashes of shapes by shape, pattern and offset */
private val dashCache = LinkedHashMap<Triple<Shape, List<Double>, Double>, Dashes>()

/**
 * The dashes of the contours of [shape] with the dash [pattern], alternating dash and gap lengths, shifted by
 * [offset], like SVG's stroke-dasharray and stroke-dashoffset. A pattern with an odd number of lengths is repeated.
 */
internal fun dashes(shape: Shape, pattern: List<Double>, offset: Double): Dashes {
    val key = Triple(shape, pattern, offset)
    dashCache[key]?.let { return it }
    val lengths = if (pattern.size % 2 == 1) pattern + pattern else pattern
    val total = lengths.sum()
    val result = mutableListOf<ShapeContour>()
    val points = mutableListOf<Pair<Vector2, Vector2>>()
    for (contour in shape.contours) {
        val length = contour.length
        if (length <= 0.0) continue
        var position = -(offset.mod(total))
        var index = 0
        while (position < length) {
            val dash = lengths[index % lengths.size]
            if (index % 2 == 0) {
                val start = max(position, 0.0)
                val end = min(position + dash, length)
                if (end > start) {
                    val t0 = contour.tForLength(start)
                    val t1 = contour.tForLength(end)
                    if (t1 > t0) result.add(contour.sub(t0, t1))
                } else if (dash == 0.0 && position >= 0.0) {
                    val t = contour.tForLength(position)
                    points.add(Pair(contour.position(t), contour.normal(t).perpendicular()))
                }
            }
            position += dash
            index++
        }
    }
    if (dashCache.size >= 256) dashCache.remove(dashCache.keys.first())
    return Dashes(result, points).also { dashCache[key] = it }
}

/** the color of [style] that [Paint.CurrentColor] refers to */
internal fun currentColor(style: Style): ColorRGBa? = (style.color as? Paint.RGB)?.value

/** the clip style of the node that is drawn, its mask is multiplied with the coverage of what is drawn */
private var activeClip: ClipStyle? = null

/**
 * A shade style that multiplies the alpha of fills and strokes by the red channel of [mask], a texture of the size of
 * the render target, read at the fragment position
 */
internal class ClipStyle(val mask: ColorBuffer) : ShadeStyle() {
    init {
        fragmentTransform = """
            float clip_coverage = texture(p_clipMask, gl_FragCoord.xy / vec2(textureSize(p_clipMask, 0))).r;
            x_fill.a *= clip_coverage;
            x_stroke.a *= clip_coverage;
        """.trimIndent()
        parameter("clipMask", mask)
    }
}

/** the shade styles combined with clip styles */
private val clippedStyles = LinkedHashMap<Pair<ShadeStyle, ClipStyle>, ShadeStyle>()

/** sets the shade style of the drawer to its shade style combined with the active clip style, if there is one */
internal fun Drawer.applyClip() {
    val clip = activeClip ?: return
    val style = shadeStyle
    shadeStyle = if (style == null || !hasCode(style)) clip else {
        clippedStyles.getOrPut(Pair(style, clip)) {
            if (clippedStyles.size >= 256) clippedStyles.remove(clippedStyles.keys.first())
            style + clip
        }
    }
}

/** the depth of the clip that is drawn, nested compositions, like those of masks and markers, continue the depth */
private var clipDepth = 0

/** the masks of clip paths by nesting depth, reused while their size matches the render target */
private val clipMasks = mutableListOf<RenderTarget>()

/** the bounds of the content of [node] in its own coordinates, null when it has none */
internal fun localBounds(node: CompositionNode): Rectangle? = when (node) {
    is ShapeNode -> node.shape.bounds.takeIf { !node.shape.empty }
    is ImageNode -> Rectangle(node.x, node.y, node.width, node.height)
    // the bounds of a text are the cells of its glyphs
    is GroupNode if node.textElement -> CompositionText.measure(node, Vector2.ZERO)
    is GroupNode -> node.children.mapNotNull { child ->
        localBounds(child)?.let { b ->
            val t = child.transform
            listOf(b.corner, b.corner + Vector2(b.width, 0.0), b.corner + Vector2(0.0, b.height), b.corner + b.dimensions)
                .map { (t * it.xy01).xy }
        }
    }.flatten().takeIf { it.isNotEmpty() }?.bounds
    else -> null
}

/**
 * Renders the mask of [clip] for [node] with the current transforms and makes it the active clip; the mask of a clip
 * in a clip is drawn clipped by the outer clip, so it is their intersection. Returns false when nothing of the node
 * is drawn: when the clip is over the bounds of a node without bounds.
 */
private fun Drawer.pushClip(clip: ClipPath.Shapes, node: CompositionNode, depth: Int): Boolean {
    val units = when (clip.units) {
        PaintUnits.OBJECT -> Matrix44.IDENTITY
        PaintUnits.BOUNDS -> {
            val bounds = localBounds(node) ?: return false
            if (bounds.width <= 0.0 || bounds.height <= 0.0) return false
            boundsTransform(bounds)
        }
    }
    val active = RenderTarget.active
    val width = (active.width * active.contentScale).toInt()
    val height = (active.height * active.contentScale).toInt()
    while (clipMasks.size <= depth) clipMasks.add(renderTarget(width, height, contentScale = 1.0) { colorBuffer(); depthBuffer() })
    var target = clipMasks[depth]
    if (target.width != width || target.height != height) {
        target.colorBuffer(0).destroy()
        target.detachColorAttachments()
        target.depthBuffer?.destroy()
        target.detachDepthBuffer()
        target.destroy()
        target = renderTarget(width, height, contentScale = 1.0) { colorBuffer(); depthBuffer() }
        clipMasks[depth] = target
    }
    val outer = activeClip
    // the projection maps the view to the render target, the mask has the same size in pixels
    isolatedWithTarget(target) {
        clear(ColorRGBa.TRANSPARENT)
        fill = ColorRGBa.WHITE
        stroke = null
        shadeStyle = outer
        model = model * units
        fun visit(clipNode: CompositionNode) {
            if (clipNode.style.display == Display.None) return
            pushModel()
            model *= clipNode.transform
            when (clipNode) {
                is ShapeNode -> if (clipNode.effectiveStyle.visibility.value) {
                    fillRule = clipNode.effectiveStyle.clipRule.value
                    shape(clipNode.shape)
                }
                is GroupNode -> clipNode.children.forEach(::visit)
                else -> {}
            }
            popModel()
        }
        clip.content.children.forEach(::visit)
    }
    activeClip = ClipStyle(target.colorBuffer(0))
    return true
}

/**
 * The vertices of [shape] where markers are drawn, with their angle along the path in degrees: [start] at the first
 * vertex, [end] at the last and [mid] at the others, like the vertices of SVG paths
 */
internal class MarkerVertices(val start: Pair<Vector2, Double>?, val mid: List<Pair<Vector2, Double>>, val end: Pair<Vector2, Double>?)

internal fun markerVertices(shape: Shape): MarkerVertices {
    val all = mutableListOf<Pair<Vector2, Double>>()
    fun angle(incoming: Vector2?, outgoing: Vector2?): Double {
        val i = incoming?.takeIf { it.length.isFinite() && it.length > 0.0 }?.normalized
        val o = outgoing?.takeIf { it.length.isFinite() && it.length > 0.0 }?.normalized
        val direction = when {
            i != null && o != null -> (i + o).let { if (it.length < 1E-9) i else it }
            else -> i ?: o ?: Vector2.UNIT_X
        }
        return atan2(direction.y, direction.x) * 180.0 / kotlin.math.PI
    }
    for (contour in shape.contours) {
        val segments = contour.segments
        if (segments.isEmpty()) continue
        val n = segments.size
        for (k in 0..n) {
            val position = if (k < n) segments[k].start else segments[n - 1].end
            val incoming = when {
                k > 0 -> segments[k - 1].direction(1.0)
                contour.closed -> segments[n - 1].direction(1.0)
                else -> null
            }
            val outgoing = when {
                k < n -> segments[k].direction(0.0)
                contour.closed -> segments[0].direction(0.0)
                else -> null
            }
            all.add(Pair(position, angle(incoming, outgoing)))
        }
    }
    if (all.isEmpty()) return MarkerVertices(null, emptyList(), null)
    return MarkerVertices(all.first(), all.subList(1, (all.size - 1).coerceAtLeast(1)), all.last().takeIf { all.size > 1 })
}

/** draws the markers of [style] at the vertices of [shape], drawn with [strokeWeight] */
private fun Drawer.drawMarkers(shape: Shape, style: Style, strokeWeight: Double) {
    val start = style.markerStart.value
    val mid = style.markerMid.value
    val end = style.markerEnd.value
    if (start == null && mid == null && end == null) return
    val vertices = markerVertices(shape)
    fun draw(marker: Marker, vertex: Pair<Vector2, Double>, isStart: Boolean) {
        val (position, angle) = vertex
        val rotation = when (val orient = marker.orient) {
            MarkerOrient.Auto -> angle
            MarkerOrient.AutoStartReverse -> if (isStart) angle + 180.0 else angle
            is MarkerOrient.Angle -> orient.degrees
        }
        val scale = if (marker.strokeWidthUnits) strokeWeight else 1.0
        isolated {
            model = model * buildTransform {
                translate(position.x, position.y, 0.0)
                rotate(Vector3.UNIT_Z, rotation)
                scale(scale, scale, 1.0)
            } * marker.contentTransform
            composition(Composition(marker.content))
        }
    }
    start?.let { m -> vertices.start?.let { draw(m, it, true) } }
    mid?.let { m -> vertices.mid.forEach { draw(m, it, false) } }
    end?.let { m -> vertices.end?.let { draw(m, it, false) } }
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

    // the model the composition is drawn with, strokes that do not scale keep their weight in its coordinates
    val baseModel = model

    // viewBox transformation
    model *= composition.calculateViewportTransform()

    // where text without a position continues
    var pen = Vector2.ZERO

    // the bounds of the text element that is drawn, when its paints need them
    var textBounds: Rectangle? = null

    fun node(compositionNode: CompositionNode) {
        // a node with display none is not drawn, nor are the nodes in it
        if (compositionNode.style.display == Display.None) return
        pushModel()
        pushStyle()
        model *= compositionNode.style.transform.value

        // a clip path is in the coordinates of the node, the content of the node is drawn through its mask
        val outerClip = activeClip
        val clipPath = compositionNode.style.clipPath
        if (clipPath is ClipPath.Shapes) {
            if (!pushClip(clipPath, compositionNode, clipDepth)) {
                popModel()
                popStyle()
                return
            }
            clipDepth++
        }

        shadeStyle = (compositionNode.style.shadeStyle as Shade.Value).value
        paintOrder = compositionNode.effectiveStyle.paintOrder.value

        // the opacity of a node on a layer of its own is applied when the layer is composited
        val ownOpacity = (compositionNode.style.opacity as? Numeric.Rational)?.value ?: 1.0
        val layered = needsLayer(compositionNode, ownOpacity)
        val opacity = if (layered) 1.0 else ownOpacity

        fun content() { when (compositionNode) {
            is ShapeNode -> {
                // fills and strokes are inherited from the groups the shape is in
                val style = compositionNode.effectiveStyle

                style.stroke.let {
                    stroke = when (it) {
                        is Paint.RGB -> it.value
                        is Paint.Server -> ColorRGBa.WHITE
                        Paint.None -> null
                        Paint.CurrentColor -> currentColor(style)
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
                    // a stroke that does not scale keeps its weight in the coordinates the composition is drawn in
                    if (style.vectorEffect == VectorEffect.NonScalingStroke) {
                        val m = baseModel.inversed * model
                        val scale = sqrt(abs(m.c0r0 * m.c1r1 - m.c0r1 * m.c1r0))
                        if (scale > 0.0) strokeWeight /= scale
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
                fillRule = style.fillRule.value
                drawStyle.smooth = style.shapeRendering.value
                style.fill.let {
                    fill = when (it) {
                        is Paint.RGB -> it.value
                        is Paint.Server -> ColorRGBa.WHITE
                        is Paint.None -> null
                        is Paint.CurrentColor -> currentColor(style)
                    }
                }
                // gradients and other paints are drawn by a paint style, which takes the alpha of the fill and stroke
                val fillServer = style.fill as? Paint.Server
                val strokeServer = style.stroke as? Paint.Server
                if (fillServer != null || strokeServer != null) {
                    val bounds = compositionNode.shape.bounds
                    // paints are prepared first, the tiles of patterns are drawn before they are read
                    val fillTransform = fillServer?.let { preparePaint(it, bounds) }
                    val strokeTransform = strokeServer?.let { preparePaint(it, bounds) }
                    if (fillServer != null && fillTransform == null) fill = null
                    if (strokeServer != null && strokeTransform == null) stroke = null
                    val paint = paintStyle(fillServer, strokeServer)
                    fillTransform?.let { paint.fillTransform = it }
                    strokeTransform?.let { paint.strokeTransform = it }
                    val own = shadeStyle
                    shadeStyle = if (own != null && hasCode(own)) own + paint else paint
                }
                style.fillOpacity.let {
                    fill = when (it) {
                        is Numeric.Rational -> fill?.opacify(it.value)
                    }
                }
                // the opacity of the groups the shape is in is applied by their layers
                stroke = stroke?.opacify(opacity)
                fill = fill?.opacify(opacity)
                if (style.visibility.value) {
                    // a dash pattern with a positive length draws the stroke as dashes, after the fill
                    // lengths along a path with a path length are in units of that length
                    val lengthScale = style.pathLength.value?.takeIf { it > 0.0 }?.let { pathLength ->
                        compositionNode.shape.contours.sumOf { it.length } / pathLength
                    } ?: 1.0
                    val pattern = style.strokeDashArray.value.map { it * lengthScale }
                    if (stroke != null && pattern.isNotEmpty() && pattern.all { it >= 0.0 } && pattern.sum() > 0.0) {
                        val strokeColor = stroke
                        val fillColor = fill
                        val strokeFirst = paintOrder == org.openrndr.draw.PaintOrder.STROKE_FILL
                        stroke = null
                        if (!strokeFirst) shape(compositionNode.shape)
                        val offset = when (val o = style.strokeDashOffset) {
                            is Length.Pixels -> o.value * lengthScale
                            is Length.Percent -> composition.normalizedDiagonalLength() * o.value / 100.0
                        }
                        val dashes = dashes(compositionNode.shape, pattern, offset)
                        fill = null
                        stroke = strokeColor
                        applyClip()
                        contours(dashes.contours)
                        // dashes of length zero are their caps: dots for round caps, squares for square caps
                        if (dashes.points.isNotEmpty() && lineCap != org.openrndr.draw.LineCap.BUTT) {
                            val half = strokeWeight / 2.0
                            fill = strokeColor
                            stroke = null
                            if (lineCap == org.openrndr.draw.LineCap.ROUND) {
                                applyClip()
                                circles(dashes.points.map { it.first }, half)
                            } else {
                                applyClip()
                                contours(dashes.points.map { (p, d) ->
                                    val n = d.perpendicular() * half
                                    val t = d * half
                                    ShapeContour.fromPoints(listOf(p - t - n, p + t - n, p + t + n, p - t + n), closed = true)
                                })
                            }
                        }
                        if (strokeFirst) {
                            fill = fillColor
                            stroke = null
                            applyClip()
                            shape(compositionNode.shape)
                        }
                    } else {
                        applyClip()
                        shape(compositionNode.shape)
                    }
                    // markers are drawn after the fill and the stroke
                    drawMarkers(compositionNode.shape, style, strokeWeight)
                }
            }
            is ImageNode -> if (compositionNode.effectiveStyle.visibility.value) {
                val image = compositionNode.image
                val source = compositionNode.source ?: Rectangle(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
                val target = Rectangle(compositionNode.x, compositionNode.y, compositionNode.width, compositionNode.height)
                if (opacity < 1.0) {
                    // scales the alpha of the image
                    drawStyle.colorMatrix = Matrix55.IDENTITY.copy(c3r3 = opacity)
                }
                applyClip()
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
                // text that wraps is drawn as lines, text along a contour is shaped and placed as a whole
                val wrapped = if (compositionNode.textElement) CompositionText.arrange(compositionNode) else null
                val contourRuns = if (wrapped == null) CompositionText.contourRuns(compositionNode) else null
                if (wrapped != null) {
                    for (run in wrapped) pen = CompositionText.draw(this, run, pen, textBounds)
                } else if (contourRuns != null) {
                    pen = CompositionText.drawOnContour(this, contourRuns, pen, textBounds)
                } else {
                    compositionNode.children.forEach { node(it) }
                }
                textBounds = outerBounds
            }
        } }
        if (layered) {
            // a node with a filter is drawn without its clip, which applies to the result of the filter
            val filtered = compositionNode.style.filter.value.isNotEmpty()
            val clip = if (filtered) activeClip?.mask else null
            if (filtered) activeClip = null
            drawLayer(compositionNode, ownOpacity, clip) { content() }
        } else {
            content()
        }
        if (clipPath is ClipPath.Shapes) clipDepth--
        activeClip = outerClip
        popModel()
        popStyle()
    }
    // clips of a composition that is drawn in another, such as the tiles of patterns, do not apply to it
    val enclosingClip = activeClip
    activeClip = null
    try {
        // nodes are blended with what is drawn under them in the composition, which is drawn on a layer of its own
        if (usesBlendModes(composition.root)) {
            drawLayer(GroupNode(), 1.0, bounded = false) { node(composition.root) }
        } else {
            node(composition.root)
        }
    } finally {
        activeClip = enclosingClip
    }
    popModel()
    popStyle()
}