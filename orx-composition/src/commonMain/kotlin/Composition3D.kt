package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.ktessellation.GLConstants.GL_TRIANGLES
import org.openrndr.ktessellation.GLConstants.GL_TRIANGLE_FAN
import org.openrndr.ktessellation.GLConstants.GL_TRIANGLE_STRIP
import org.openrndr.ktessellation.GLU
import org.openrndr.ktessellation.Tessellator
import org.openrndr.math.Matrix44
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.*
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.shadestyles.presets.boundsTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The triangles of the fill and the stroke of a [ShapeNode], kept with the shape and the parameters they were made
 * for, so that a composition that does not change is tessellated once
 */
internal class NodeTessellation(val shape: Shape, val key: Any, val triangles: List<Vector2>)

/**
 * Draws [composition] in 3D, like [composition] draws it in 2D, but as triangles: fills are tessellated with
 * openrndr-ktessellation, and strokes are made of the outlines of their segments, joins and caps in the plane of the
 * composition, which are tessellated together so that a translucent stroke is drawn once where it overlaps itself.
 * The composition is in the xy-plane of the model of the drawer, so it can be placed, turned and seen in perspective
 * like any other geometry, with depth testing. Its layers are drawn in their order, without fighting over depth; when
 * the drawer writes depth, the composition is drawn a second time to the depth buffer only.
 *
 * Fills and strokes are drawn in their colors and opacities, gradients and patterns in object units, with the line
 * joins, caps, miter limits and dash patterns of the strokes. A stroke that does not scale is drawn as a line of its
 * width on the screen, also with its joins and caps. Text, also text that wraps and text along a contour, is drawn as
 * the shapes of its glyphs, with the colors, gradients and patterns of their fills and strokes; images are drawn as
 * [composition] draws them. Clip paths and masks are rendered with the transforms of the composition into textures of
 * the size of the target, by which the alpha of what is drawn in them is multiplied, so they clip in perspective too;
 * a mask applies to every shape in it, not to the group of them as a whole. A node with filters or a blend mode is
 * drawn flat, as [composition] draws it, on a layer of the resolution it has on the screen, which is drawn in the
 * plane and blended with what is drawn under it; the layer covers the part of the node on the screen and the margin
 * its filters reach into, a node that is not on the screen is not drawn. A composition with blend modes is drawn on a
 * layer of its own, so that it blends with itself only, like in SVG. Markers are drawn at the vertices of their
 * shapes, their content in 3D too. Group opacity is applied to the shapes in the group.
 *
 * The tessellations are kept with the shapes and made again when a shape or its stroke changes.
 *
 * @param tolerance the largest distance between the curves and the triangles, in the units of the composition
 */
fun Drawer.composition3d(composition: Composition, tolerance: Double = 0.25) {
    // the fills and strokes are in one plane, where the depths of their triangles differ in the last bits: they are
    // drawn in their order without writing depth, against the depth of what is drawn before them; when depth is
    // written, the composition is drawn once more to the depth only, to hide what is drawn after it behind it
    val writesDepth = drawStyle.depthWrite
    pushStyle()
    drawStyle.depthWrite = false
    if (usesBlendModes(composition.root)) {
        // nodes are blended with what is drawn under them in the composition, which is drawn on a layer of its own
        rootLayer3d(composition, tolerance)
    } else {
        composition3dPass(composition, tolerance)
    }
    if (writesDepth) {
        drawStyle.depthWrite = true
        drawStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
        // what clips and masks leave out does not hide what is behind it
        clip3dDiscards = true
        try {
            composition3dPass(composition, tolerance)
        } finally {
            clip3dDiscards = false
        }
    }
    popStyle()
}

private fun Drawer.composition3dPass(composition: Composition, tolerance: Double, opacity: Double = 1.0) {
    pushModel()
    pushStyle()
    val baseModel = model
    model *= composition.calculateViewportTransform()

    var pen = Vector2.ZERO

    // the bounds of the text element that is drawn, when its paints need them
    var textBounds: Rectangle? = null

    fun node(compositionNode: CompositionNode, outerOpacity: Double) {
        if (compositionNode.style.display == Display.None) return
        pushModel()
        pushStyle()
        model *= compositionNode.style.transform.value
        shadeStyle = (compositionNode.style.shadeStyle as Shade.Value).value
        val opacity = outerOpacity * ((compositionNode.style.opacity as? Numeric.Rational)?.value ?: 1.0)
        // the scale of the node in the composition, for tolerances and strokes that do not scale
        val m = baseModel.inversed * model
        val scale = sqrt(abs(m.c0r0 * m.c1r1 - m.c0r1 * m.c1r0)).takeIf { it > 1E-12 } ?: 1.0

        // the clip path and the mask of the node are the coverage of what is drawn in it
        val outerClip = clip3d
        val outerDepth = clip3dDepth
        fun restore() {
            clip3d = outerClip
            clip3dDepth = outerDepth
        }
        val clipPath = compositionNode.style.clipPath
        val mask = (compositionNode.style.mask as? MaskReference.Value)?.value
        if ((clipPath is ClipPath.Shapes && !pushClip3d(clipPath, compositionNode, tolerance / scale)) ||
            (mask != null && !pushMask3d(mask, compositionNode, tolerance))) {
            restore()
            popModel()
            popStyle()
            return
        }

        // a node with filters or a blend mode is drawn flat on a layer, which is drawn in the plane; the pass that
        // writes depth draws it as it is
        val filtered = compositionNode.style.filter.value.isNotEmpty()
        val blended = compositionNode.style.mixBlendMode != MixBlendMode.NORMAL
        if ((filtered || blended) && !clip3dDiscards) {
            layer3d(composition, compositionNode, outerOpacity)
            restore()
            popModel()
            popStyle()
            return
        }

        when (compositionNode) {
            is ShapeNode -> if (compositionNode.effectiveStyle.visibility.value) {
                shape3d(composition, compositionNode, opacity, tolerance, scale)
            }
            is ImageNode -> if (compositionNode.effectiveStyle.visibility.value) {
                val image = compositionNode.image
                val source = compositionNode.source ?: Rectangle(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
                if (opacity < 1.0) drawStyle.colorMatrix = Matrix55.IDENTITY.copy(c3r3 = opacity)
                applyClip3d()
                image(image, source, Rectangle(compositionNode.x, compositionNode.y, compositionNode.width, compositionNode.height))
            }
            is TextNode -> pen = text3d(listOf(compositionNode), pen, opacity, tolerance / scale, textBounds)
            is GroupNode -> {
                // as composition draws text: paints over the bounds of text are mapped to the bounds of the whole text
                // element, text that wraps is drawn as lines, text along a contour is placed as a whole
                val outerBounds = textBounds
                if (compositionNode.textElement) {
                    val runs = CompositionText.textRuns(compositionNode)
                    if (runs != null && CompositionText.hasPaints(runs)) textBounds = CompositionText.measure(compositionNode, pen)
                }
                val wrapped = if (compositionNode.textElement) CompositionText.arrange(compositionNode) else null
                val contourRuns = if (wrapped == null) CompositionText.contourRuns(compositionNode) else null
                when {
                    wrapped != null -> for (run in wrapped) pen = text3d(listOf(run), pen, opacity, tolerance / scale, textBounds)
                    contourRuns != null -> pen = text3d(contourRuns, pen, opacity, tolerance / scale, textBounds)
                    else -> compositionNode.children.forEach { node(it, opacity) }
                }
                textBounds = outerBounds
            }
        }
        restore()
        popModel()
        popStyle()
    }
    node(composition.root, opacity)
    popModel()
    popStyle()
}

private fun Drawer.shape3d(composition: Composition, node: ShapeNode, opacity: Double, tolerance: Double, scale: Double) {
    val unresolved = node.effectiveStyle
    val style = resolveContextPaints(unresolved)
    val shape = node.shape
    fun color(paint: Paint) = when (paint) {
        is Paint.RGB -> paint.value
        is Paint.Server -> ColorRGBa.WHITE
        Paint.CurrentColor -> currentColor(style)
        Paint.None, Paint.ContextFill, Paint.ContextStroke -> null
    }
    val fillColor = color(style.fill)?.opacify((style.fillOpacity as Numeric.Rational).value * opacity)
    val strokeColor = color(style.stroke)?.opacify((style.strokeOpacity as Numeric.Rational).value * opacity)
    val own = shadeStyle
    val localTolerance = tolerance / scale

    fun paintStyleFor(server: Paint.Server?, original: Paint): ShadeStyle? {
        if (server == null) return own
        val transform = preparePaint(server, original, shape.bounds) ?: return null
        val paint = paintStyle(server, null).also { it.fillTransform = transform }
        return if (own != null && hasShadeCode(own)) own + paint else paint
    }

    fun draw(fillWith: ColorRGBa, style3d: ShadeStyle?, triangles: List<Vector2>) {
        if (triangles.isEmpty() || fillWith.alpha <= 0.0) return
        fill = fillWith
        stroke = null
        shadeStyle = style3d
        triangles3d(triangles)
    }

    val strokeFirst = style.paintOrder.value == org.openrndr.draw.PaintOrder.STROKE_FILL

    fun fill() {
        val c = fillColor ?: return
        val server = style.fill as? Paint.Server
        val s = if (server != null) paintStyleFor(server, unresolved.fill) ?: return else own
        val rule = if (style.fillRule.value.name == "EVEN_ODD") org.openrndr.shape.FillRule.EVEN_ODD else org.openrndr.shape.FillRule.NON_ZERO
        val key = Triple("fill", rule, localTolerance)
        val cached = node.fillTessellation?.takeIf { it.shape === shape && it.key == key }
        val triangles = cached?.triangles ?: fillTriangles(shape, localTolerance, rule).also {
            node.fillTessellation = NodeTessellation(shape, key, it)
        }
        draw(c, s, triangles)
    }

    fun stroke() {
        val c = strokeColor ?: return
        var weight = when (val w = style.strokeWeight) {
            is Length.Pixels -> w.value
            is Length.Percent -> composition.normalizedDiagonalLength() * w.value / 100.0
        }
        if (weight <= 0.0) return
        val lengthScale = style.pathLength.value?.takeIf { it > 0.0 }?.let { pathLength -> shape.contours.sumOf { it.length } / pathLength } ?: 1.0
        val pattern = style.strokeDashArray.value.map { it * lengthScale }
        val dashed = pattern.isNotEmpty() && pattern.all { it >= 0.0 } && pattern.sum() > 0.0
        val offset = when (val o = style.strokeDashOffset) {
            is Length.Pixels -> o.value * lengthScale
            is Length.Percent -> composition.normalizedDiagonalLength() * o.value / 100.0
        }
        val contours = if (dashed) dashes(shape, pattern, offset).contours else shape.contours

        if (style.vectorEffect == VectorEffect.NonScalingStroke) {
            // a stroke that does not scale is a line of its width on the screen, with its joins and caps, blended once
            // where it overlaps itself
            fill = null
            stroke = c
            strokeWeight = weight
            shadeStyle = own
            lineJoin = style.lineJoin.value
            lineCap = style.lineCap.value
            miterLimit = (style.miterLimit as Numeric.Rational).value
            drawStyle.lineBlendOnce = c.alpha < 1.0
            drawStyle.lineWidthMode = LineWidthMode.SCREEN
            val strips = contours.map { contour ->
                contour.adaptivePositions(localTolerance).map { Vector3(it.x, it.y, 0.0) }.let { if (contour.closed) it.dropLast(1) else it }
            }
            val drawn = strips.indices.filter { strips[it].size >= 2 }
            applyClip3d()
            if (drawn.isNotEmpty()) lineStrips(drawn.map { strips[it] }, drawn.map { contours[it].closed })
            return
        }
        val server = style.stroke as? Paint.Server
        val s = if (server != null) paintStyleFor(server, unresolved.stroke) ?: return else own
        val join = style.lineJoin.value
        val cap = style.lineCap.value
        val miter = (style.miterLimit as Numeric.Rational).value
        val key = listOf("stroke", weight, join, cap, miter, localTolerance, pattern, offset)
        val cached = node.strokeTessellation?.takeIf { it.shape === shape && it.key == key }
        val triangles = cached?.triangles ?: strokeTriangles(contours, weight, join, cap, miter, localTolerance).also {
            node.strokeTessellation = NodeTessellation(shape, key, it)
        }
        draw(c, s, triangles)
    }

    if (strokeFirst) { stroke(); fill() } else { fill(); stroke() }

    // the markers, after the fill and the stroke, their content in 3D too
    if (style.markerStart.value != null || style.markerMid.value != null || style.markerEnd.value != null) {
        val weight = when (val w = style.strokeWeight) {
            is Length.Pixels -> w.value
            is Length.Percent -> composition.normalizedDiagonalLength() * w.value / 100.0
        }
        val outer = model
        shadeStyle = own
        forEachMarker(shape, style, weight) { marker ->
            // the tolerance is in the units of the composition, the content of a marker is scaled
            val m = outer.inversed * model
            val markerScale = sqrt(abs(m.c0r0 * m.c1r1 - m.c0r1 * m.c1r0)).takeIf { it > 1E-12 } ?: 1.0
            composition3dPass(Composition(marker.content), localTolerance / markerScale, opacity)
        }
    }
}

/** triangles of glyphs in [color], of their strokes when [stroke] is set, of their fills otherwise */
internal class GlyphPart(val stroke: Boolean, val color: ColorRGBa, val triangles: List<Vector2>)

/** the triangles of the glyphs of text, by group of glyphs that share paints, for the glyphs and places in [key] */
internal class TextTessellation(val key: Any, val groups: List<List<GlyphPart>>)

/**
 * Draws the glyphs of [runs] from [pen] as shapes: a run on its own, or runs along their contour. The fills and
 * strokes of the glyphs are triangles in their colors with [opacity], their gradients and patterns are mapped to
 * [textBounds], the bounds of the text element, or to the bounds of the glyphs. Returns where text continues.
 */
private fun Drawer.text3d(runs: List<TextNode>, pen: Vector2, opacity: Double, tolerance: Double, textBounds: Rectangle?): Vector2 {
    val first = runs.first()
    val (groups, glyphBounds, end) = (if (first.contour != null) CompositionText.contourGlyphGroups(runs, pen)
        else CompositionText.glyphGroups(first, pen)) ?: return pen
    // hidden runs are laid out, the text after them continues after them
    if (first.contour == null && !first.effectiveStyle.visibility.value) return end
    val key = listOf(groups.map { g -> g.outlines.map { it.key } }, tolerance)
    val tessellated = first.tessellation?.takeIf { it.key == key }?.groups ?: groups.map { group ->
        val byColor = LinkedHashMap<Pair<Boolean, ColorRGBa>, MutableList<Vector2>>()
        for (glyph in group.outlines) {
            val shape = glyph.shape()
            glyph.fill?.let { c -> byColor.getOrPut(false to c) { mutableListOf() }.addAll(fillTriangles(shape, tolerance, org.openrndr.shape.FillRule.NON_ZERO)) }
            glyph.stroke?.let { c ->
                byColor.getOrPut(true to c) { mutableListOf() }.addAll(
                    strokeTriangles(shape.contours, glyph.strokeWeight ?: 1.0, org.openrndr.draw.LineJoin.MITER, org.openrndr.draw.LineCap.BUTT, 4.0, tolerance)
                )
            }
        }
        byColor.map { (k, triangles) -> GlyphPart(k.first, k.second, triangles) }
    }.also { first.tessellation = TextTessellation(key, it) }

    val own = shadeStyle
    stroke = null
    val bounds = textBounds ?: glyphBounds ?: Rectangle(0.0, 0.0, 0.0, 0.0)
    for ((group, parts) in groups.zip(tessellated)) {
        // the paints of the group, a paint over empty bounds is not drawn
        val (fillServer, strokeServer) = CompositionText.servers(group.style) ?: (null to null)
        fun styleFor(server: Paint.Server?): ShadeStyle? {
            server ?: return own
            val transform = preparePaint(server, bounds) ?: return null
            val paint = paintStyle(server, null).also { it.fillTransform = transform }
            return if (own != null && hasShadeCode(own)) own + paint else paint
        }
        val fillStyle = styleFor(fillServer)
        val strokeStyle = styleFor(strokeServer)
        for (part in parts) {
            if (part.triangles.isEmpty()) continue
            val server = if (part.stroke) strokeServer else fillServer
            val style = if (part.stroke) strokeStyle else fillStyle
            if (server != null && style == null) continue
            shadeStyle = style
            fill = part.color.opacify(opacity)
            triangles3d(part.triangles)
        }
    }
    shadeStyle = own
    return end
}

/** true when [style] has code */
private fun hasShadeCode(style: ShadeStyle): Boolean =
    !style.fragmentTransform.isNullOrBlank() || !style.vertexTransform.isNullOrBlank() ||
            !style.fragmentPreamble.isNullOrBlank() || !style.vertexPreamble.isNullOrBlank()

//<editor-fold desc="clips and masks">
/** the coverage of the clip paths and masks that what is drawn is in, in the red channel, null when it is in none */
private var clip3d: ColorBuffer? = null

/** the depth of nested clips and masks, which take turns with their textures */
private var clip3dDepth = 0

/** true in the pass that writes depth: fragments that clips leave out are discarded */
private var clip3dDiscards = false

/** the coverage textures and the textures of the content of masks, by depth */
private val coverageTargets = mutableListOf<RenderTarget>()
private val maskContentTargets = mutableListOf<RenderTarget>()

/** a render target of the size of the active target from [pool] at [depth] */
private fun pooled(pool: MutableList<RenderTarget>, depth: Int): RenderTarget {
    val active = RenderTarget.active
    val width = (active.width * active.contentScale).toInt()
    val height = (active.height * active.contentScale).toInt()
    while (pool.size <= depth) pool.add(renderTarget(width, height, contentScale = 1.0) { colorBuffer() })
    val target = pool[depth]
    if (target.width == width && target.height == height) return target
    target.colorBuffer(0).destroy()
    target.detachColorAttachments()
    target.destroy()
    return renderTarget(width, height, contentScale = 1.0) { colorBuffer() }.also { pool[depth] = it }
}

/** multiplies the alpha of fills and strokes by the coverage of a clip, or discards what it does not cover */
private class Clip3DStyle(coverage: ColorBuffer, discards: Boolean) : ShadeStyle() {
    init {
        fragmentTransform = """
            float clip_coverage = texture(p_coverage, gl_FragCoord.xy / vec2(textureSize(p_coverage, 0))).r;
            ${if (discards) "if (clip_coverage < 0.5) discard;" else "x_fill.a *= clip_coverage; x_stroke.a *= clip_coverage;"}
        """.trimIndent()
        parameter("coverage", coverage)
    }
}

private val clip3dStyles = HashMap<Pair<ColorBuffer, Boolean>, Clip3DStyle>()
private val clipped3dStyles = LinkedHashMap<Pair<ShadeStyle, Clip3DStyle>, ShadeStyle>()

/** sets the shade style of the drawer to its shade style combined with the clip that is active, if one is */
private fun Drawer.applyClip3d() {
    val coverage = clip3d ?: return
    val clip = clip3dStyles.getOrPut(coverage to clip3dDiscards) { Clip3DStyle(coverage, clip3dDiscards) }
    val style = shadeStyle
    shadeStyle = if (style == null || !hasShadeCode(style)) clip else clipped3dStyles.getOrPut(style to clip) {
        if (clipped3dStyles.size >= 256) clipped3dStyles.remove(clipped3dStyles.keys.first())
        style + clip
    }
}

/** draws into [target], cleared, with the transforms of the drawer, with colors, without depth */
private fun Drawer.intoTarget(target: RenderTarget, draw: Drawer.() -> Unit) {
    isolatedWithTarget(target) {
        clear(ColorRGBa.TRANSPARENT)
        drawStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
        drawStyle.depthWrite = false
        drawStyle.depthTestPass = DepthTestPass.ALWAYS
        drawStyle.blendMode = BlendMode.OVER
        drawStyle.colorMatrix = Matrix55.IDENTITY
        val discards = clip3dDiscards
        clip3dDiscards = false
        try {
            draw()
        } finally {
            clip3dDiscards = discards
        }
    }
}

/**
 * Renders the coverage of [clip] for [node] with the transforms of the drawer and makes it the clip, within the clip
 * that is active; false when nothing of the node is drawn, when the clip is over the bounds of a node without them
 */
private fun Drawer.pushClip3d(clip: ClipPath.Shapes, node: CompositionNode, tolerance: Double): Boolean {
    val units = when (clip.units) {
        PaintUnits.OBJECT -> Matrix44.IDENTITY
        PaintUnits.BOUNDS -> {
            val bounds = localBounds(node) ?: return false
            if (bounds.width <= 0.0 || bounds.height <= 0.0) return false
            boundsTransform(bounds)
        }
    }
    val target = pooled(coverageTargets, clip3dDepth)
    intoTarget(target) {
        fill = ColorRGBa.WHITE
        stroke = null
        shadeStyle = null
        model *= units
        fun visit(clipNode: CompositionNode) {
            if (clipNode.style.display == Display.None) return
            pushModel()
            model *= clipNode.transform
            when (clipNode) {
                is ShapeNode -> if (clipNode.effectiveStyle.visibility.value) {
                    val rule = if (clipNode.effectiveStyle.clipRule.value.name == "EVEN_ODD") org.openrndr.shape.FillRule.EVEN_ODD
                    else org.openrndr.shape.FillRule.NON_ZERO
                    // the outer clip applies to the triangles, the coverage is the intersection of the clips
                    triangles3d(fillTriangles(clipNode.shape, tolerance, rule))
                }
                is GroupNode -> clipNode.children.forEach(::visit)
                else -> {}
            }
            popModel()
        }
        clip.content.children.forEach(::visit)
    }
    clip3d = target.colorBuffer(0)
    clip3dDepth++
    return true
}

/** the value of a mask from the colors of its content, which are premultiplied and linear, like composition masks */
private class MaskValueStyle : ShadeStyle() {
    init {
        fragmentTransform = """
            vec4 m = texture(p_content, gl_FragCoord.xy / vec2(textureSize(p_content, 0)));
            float v;
            if (p_luminance == 1) {
                vec3 c = clamp(m.a > 0.0 ? m.rgb / m.a : vec3(0.0), 0.0, 1.0);
                c = mix(1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, c * 12.92, step(c, vec3(0.0031308)));
                v = dot(c, vec3(0.2125, 0.7154, 0.0721)) * m.a;
            } else {
                v = m.a;
            }
            x_fill = vec4(v, 0.0, 0.0, 1.0);
        """.trimIndent()
    }
}

private val maskValueStyle by lazy { MaskValueStyle() }

/**
 * Renders the content of [mask] for [node] in 3D with the transforms of the drawer, in its region, and makes its value
 * the clip, within the clip that is active; false when nothing of the node is drawn, see [Drawer.composition]
 */
private fun Drawer.pushMask3d(mask: Mask, node: CompositionNode, tolerance: Double): Boolean {
    val bounds = localBounds(node)
    val usable = bounds != null && bounds.width > 0.0 && bounds.height > 0.0
    if ((mask.units == PaintUnits.BOUNDS || (mask.region != null && mask.regionUnits == PaintUnits.BOUNDS)) && !usable) {
        return false
    }
    // the content in its units, clipped to the region, as composition draws masks
    val content = GroupNode().also { group ->
        group.style = mask.content.style
        mask.content.children.forEach { group.children.add(it) }
        if (mask.units == PaintUnits.BOUNDS) group.transform = boundsTransform(bounds!!)
    }
    val root = GroupNode().also { root ->
        root.children.add(content)
        mask.region?.let { region ->
            val rectangle = if (mask.regionUnits == PaintUnits.BOUNDS) Rectangle(
                bounds!!.x + region.x * bounds.width, bounds.y + region.y * bounds.height,
                region.width * bounds.width, region.height * bounds.height
            ) else region
            val clip = GroupNode()
            clip.children.add(ShapeNode(rectangle.shape).also { it.parent = clip })
            root.style.clipPath = ClipPath.Shapes(clip)
        }
    }
    val depth = clip3dDepth
    val contentTarget = pooled(maskContentTargets, depth)
    // the clip that is active applies to the content, its value is within that clip
    clip3dDepth++
    try {
        intoTarget(contentTarget) { composition3dPass(Composition(root), tolerance) }
    } finally {
        // the children of the mask are not moved to the temporary groups
        mask.content.children.forEach { it.parent = mask.content }
    }
    val coverage = pooled(coverageTargets, depth)
    isolatedWithTarget(coverage) {
        defaults()
        ortho(coverage)
        drawStyle.blendMode = BlendMode.REPLACE
        maskValueStyle.parameter("content", contentTarget.colorBuffer(0))
        maskValueStyle.parameter("luminance", if (mask.type == MaskType.LUMINANCE) 1 else 0)
        shadeStyle = maskValueStyle
        stroke = null
        fill = ColorRGBa.WHITE
        rectangle(0.0, 0.0, coverage.width.toDouble(), coverage.height.toDouble())
    }
    clip3d = coverage.colorBuffer(0)
    clip3dDepth = depth + 1
    return true
}
//</editor-fold>

//<editor-fold desc="layers">
/** the idle layers of nodes with filters and blend modes, by size, oldest first, see [layer3d] */
private val idleLayers = LinkedHashMap<Pair<Int, Int>, MutableList<RenderTarget>>()
private const val MAX_IDLE_LAYERS = 12

/**
 * A render target of at least [width] by [height] pixels: sizes are rounded up to 64 pixels, so that layers of nodes
 * of similar sizes, and of the same nodes from frame to frame, share their targets. Given back with [releaseLayer].
 */
private fun acquireLayer(width: Int, height: Int): RenderTarget {
    val key = Pair((width + 63) / 64 * 64, (height + 63) / 64 * 64)
    idleLayers[key]?.let { list ->
        val target = list.removeAt(list.lastIndex)
        if (list.isEmpty()) idleLayers.remove(key)
        return target
    }
    return renderTarget(key.first, key.second, contentScale = 1.0) { colorBuffer(type = ColorType.FLOAT16); depthBuffer() }
}

private fun releaseLayer(target: RenderTarget) {
    val key = Pair(target.width, target.height)
    // the most recent size goes to the end, the oldest idle layers go when there are too many
    val list = idleLayers.remove(key) ?: mutableListOf()
    list.add(target)
    idleLayers[key] = list
    while (idleLayers.values.sumOf { it.size } > MAX_IDLE_LAYERS) {
        val oldest = idleLayers.keys.first()
        val targets = idleLayers.getValue(oldest)
        val t = targets.removeAt(0)
        if (targets.isEmpty()) idleLayers.remove(oldest)
        t.colorBuffer(0).destroy(); t.detachColorAttachments(); t.depthBuffer?.destroy(); t.detachDepthBuffer(); t.destroy()
    }
}

/**
 * The part of the plane z = 0 of the model that is on the screen, the bounds of where the rays through the corners of
 * the screen meet it, null when one does not, like when the horizon of the plane is on the screen
 */
private fun Drawer.visiblePlane(): Rectangle? {
    val toModel = (projection * view * model).inversed
    val hits = listOf(Vector2(-1.0, -1.0), Vector2(1.0, -1.0), Vector2(1.0, 1.0), Vector2(-1.0, 1.0)).map { ndc ->
        val near = (toModel * org.openrndr.math.Vector4(ndc.x, ndc.y, -1.0, 1.0)).let { it.xyz / it.w }
        val far = (toModel * org.openrndr.math.Vector4(ndc.x, ndc.y, 1.0, 1.0)).let { it.xyz / it.w }
        val dz = far.z - near.z
        if (abs(dz) < 1E-12) return null
        val t = -near.z / dz
        // the plane is met in front of the camera, between the near and the far plane
        if (t < 0.0 || t > 1.0) return null
        (near + (far - near) * t).xy
    }
    return hits.bounds
}

private fun intersection(a: Rectangle, b: Rectangle): Rectangle? {
    val x0 = maxOf(a.x, b.x)
    val y0 = maxOf(a.y, b.y)
    val x1 = minOf(a.x + a.width, b.x + b.width)
    val y1 = minOf(a.y + a.height, b.y + b.height)
    return if (x1 <= x0 || y1 <= y0) null else Rectangle(x0, y0, x1 - x0, y1 - y0)
}

/** reads a layer at the position in the plane: [p_region] is the rectangle of the layer in the coordinates of the node */
private class LayerQuadStyle : ShadeStyle() {
    init {
        fragmentTransform = """
            vec2 layer_quad_uv = (v_objectPosition.xy - p_region.xy) / p_region.zw;
            x_fill = texture(p_layer, vec2(layer_quad_uv.x, 1.0 - layer_quad_uv.y));
        """.trimIndent()
    }
}

/** reads a layer of the size of the target at the fragment position */
private class ScreenLayerStyle : ShadeStyle() {
    init {
        fragmentTransform = "x_fill = texture(p_layer, gl_FragCoord.xy / vec2(textureSize(p_layer, 0)));"
    }
}

private val layerQuadStyle by lazy { LayerQuadStyle() }
private val screenLayerStyle by lazy { ScreenLayerStyle() }
private val layerComposites = HashMap<Pair<ShadeStyle, ShadeStyle>, ShadeStyle>()

private fun composed(a: ShadeStyle, b: ShadeStyle) = layerComposites.getOrPut(a to b) { a + b }

/** the corners of [r], two triangles */
private fun quad(r: Rectangle): List<Vector2> {
    val a = r.corner
    val b = Vector2(r.x + r.width, r.y)
    val c = Vector2(r.x + r.width, r.y + r.height)
    val d = Vector2(r.x, r.y + r.height)
    return listOf(a, b, c, a, c, d)
}

/**
 * Draws [node], which has filters or a blend mode, flat on a layer, as [composition] draws it, and the layer on a
 * rectangle in the plane of the node, the region its filters draw in. The layer has the resolution of the region on
 * the screen. It is composited with [opacity], the opacity of the groups the node is in, the clip and the mask that
 * are active, and blended with what is under it by the blend mode of the node.
 */
private fun Drawer.layer3d(composition: Composition, node: CompositionNode, opacity: Double) {
    // filters that draw everywhere draw in the bounds of the node and a margin
    val content = localBounds(node)
    val region = visualBounds(node) ?: content?.let { it.offsetEdges(max(it.width, it.height) * 0.1) } ?: return
    if (region.width <= 0.0 || region.height <= 0.0) return
    // only the part of the region on the screen is drawn, with the margin that the filters reach into from around it;
    // a node that is not on the screen is not drawn at all
    val reach = if (content == null) max(region.width, region.height) * 0.1 else maxOf(
        content.x - region.x, region.x + region.width - content.x - content.width,
        content.y - region.y, region.y + region.height - content.y - content.height, 0.0
    )
    // what the node draws is in its region: a node of which the region is not on the screen shows nothing, the filters
    // of a node that shows read what is around the part that is on the screen
    val layerRegion = visiblePlane()?.let { visible ->
        intersection(region, visible) ?: return
        intersection(region, visible.offsetEdges(reach)) ?: return
    } ?: region
    // the size of the region on the screen, in pixels; corners behind the camera take the largest layer
    val active = RenderTarget.active
    val pw = active.width * active.contentScale
    val ph = active.height * active.contentScale
    val toClip = projection * view * model
    val corners = listOf(layerRegion.corner, Vector2(layerRegion.x + layerRegion.width, layerRegion.y),
        Vector2(layerRegion.x, layerRegion.y + layerRegion.height), layerRegion.corner + layerRegion.dimensions).map { p ->
        val c = toClip * p.xy01
        if (c.w <= 1E-6) null else Vector2((c.x / c.w * 0.5 + 0.5) * pw, (c.y / c.w * 0.5 + 0.5) * ph)
    }
    val maxSize = 4096
    val (width, height) = if (corners.any { it == null }) maxSize to maxSize else {
        val (a, b, c, d) = corners.map { it!! }
        val w = maxOf(a.distanceTo(b), c.distanceTo(d))
        val h = maxOf(a.distanceTo(c), b.distanceTo(d))
        ceil(w).toInt().coerceIn(1, maxSize) to ceil(h).toInt().coerceIn(1, maxSize)
    }
    val target = acquireLayer(width, height)
    try {
        // the layer fills its target, which can be a little larger than the region needs
        val tw = target.width.toDouble()
        val th = target.height.toDouble()
        isolatedWithTarget(target) {
            clear(ColorRGBa.TRANSPARENT)
            ortho(target)
            view = Matrix44.IDENTITY
            model = org.openrndr.math.transforms.buildTransform {
                scale(tw / layerRegion.width, th / layerRegion.height, 1.0)
                translate(-layerRegion.x, -layerRegion.y, 0.0)
            }
            drawStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            drawStyle.depthWrite = false
            drawStyle.depthTestPass = DepthTestPass.ALWAYS
            drawStyle.blendMode = BlendMode.OVER
            drawStyle.colorMatrix = Matrix55.IDENTITY
            shadeStyle = null
            compositionFrom(composition, node, flat = true)
        }
        val mode = node.style.mixBlendMode
        val backdrop = if (mode == MixBlendMode.NORMAL) null else copyBackdrop()
        layerQuadStyle.parameter("layer", target.colorBuffer(0))
        layerQuadStyle.parameter("region", org.openrndr.math.Vector4(layerRegion.x, layerRegion.y, layerRegion.width, layerRegion.height))
        val composite = layerCompositeStyle(opacity, if (backdrop == null) MixBlendMode.NORMAL else mode, backdrop, clip3d)
        isolated {
            shadeStyle = composed(layerQuadStyle, composite)
            // a blended layer replaces the backdrop that it is blended with
            drawStyle.blendMode = if (backdrop != null) BlendMode.REPLACE else BlendMode.OVER
            fill = ColorRGBa.WHITE
            stroke = null
            triangles3d(quad(layerRegion), clipped = false)
        }
    } finally {
        releaseLayer(target)
    }
}

/** the layer of a composition with blend modes, of the size of the target */
private var rootLayer: RenderTarget? = null

/**
 * Draws [composition], which has blend modes, on a layer of the size of the target and the layer on the rectangle of
 * the viewport of the composition, in its plane, so that what is drawn before it in front of it hides it
 */
private fun Drawer.rootLayer3d(composition: Composition, tolerance: Double) {
    val active = RenderTarget.active
    val width = (active.width * active.contentScale).toInt()
    val height = (active.height * active.contentScale).toInt()
    val target = rootLayer?.takeIf { it.width == width && it.height == height } ?: run {
        rootLayer?.let { t -> t.colorBuffer(0).destroy(); t.detachColorAttachments(); t.depthBuffer?.destroy(); t.detachDepthBuffer(); t.destroy() }
        renderTarget(width, height, contentScale = 1.0) { colorBuffer(type = ColorType.FLOAT16); depthBuffer() }.also { rootLayer = it }
    }
    isolatedWithTarget(target) {
        clear(ColorRGBa.TRANSPARENT)
        drawStyle.depthTestPass = DepthTestPass.ALWAYS
        composition3dPass(composition, tolerance)
    }
    val (x, y) = composition.bounds.position
    val viewport = Rectangle(x, y, composition.bounds.dimensions.x, composition.bounds.dimensions.y)
    screenLayerStyle.parameter("layer", target.colorBuffer(0))
    isolated {
        shadeStyle = screenLayerStyle
        fill = ColorRGBa.WHITE
        stroke = null
        drawStyle.blendMode = BlendMode.OVER
        triangles3d(quad(viewport), clipped = false)
    }
}
//</editor-fold>

//<editor-fold desc="triangles">
private val triangleFormat = vertexFormat { position(3) }
private val triangleBuffers = mutableListOf<VertexBuffer>()
private var nextBuffer = 0

/** draws [triangles], corners in the xy-plane, in the fill of the drawer */
private fun Drawer.triangles3d(triangles: List<Vector2>, clipped: Boolean = true) {
    // a few buffers in turn, so that a buffer is not written while it is drawn
    if (triangleBuffers.size < 4) repeat(4) { triangleBuffers.add(vertexBuffer(triangleFormat, 4096, Session.root)) }
    val index = nextBuffer++ % triangleBuffers.size
    var buffer = triangleBuffers[index]
    if (buffer.vertexCount < triangles.size) {
        buffer.destroy()
        var size = buffer.vertexCount
        while (size < triangles.size) size *= 2
        buffer = vertexBuffer(triangleFormat, size, Session.root)
        triangleBuffers[index] = buffer
    }
    buffer.put {
        for (p in triangles) write(Vector3(p.x, p.y, 0.0))
    }
    if (clipped) applyClip3d()
    vertexBuffer(buffer, DrawPrimitive.TRIANGLES, 0, triangles.size)
}

/** the corners of the triangles of the primitives of [tessellator] */
private fun triangles(tessellator: Tessellator): List<Vector2> {
    val result = mutableListOf<Vector2>()
    for (primitive in tessellator.primitives) {
        val p = primitive.positions
        if (p.size < 3) continue
        when (primitive.type) {
            GL_TRIANGLES -> result.addAll(p)
            GL_TRIANGLE_FAN -> for (i in 1 until p.size - 1) { result.add(p[0]); result.add(p[i]); result.add(p[i + 1]) }
            GL_TRIANGLE_STRIP -> for (i in 0 until p.size - 2) {
                if (i % 2 == 0) { result.add(p[i]); result.add(p[i + 1]); result.add(p[i + 2]) }
                else { result.add(p[i + 1]); result.add(p[i]); result.add(p[i + 2]) }
            }
        }
    }
    return result
}

/** the triangles of the fill of [shape]: its contours, the open ones closed, by [rule] */
internal fun fillTriangles(shape: Shape, tolerance: Double, rule: org.openrndr.shape.FillRule): List<Vector2> {
    val closed = shape.contours.filter { !it.empty }.map { c ->
        if (c.closed) c else ShapeContour(c.segments + Segment2D(c.segments.last().end, c.segments.first().start), true)
    }
    if (closed.isEmpty()) return emptyList()
    return triangulate(Shape(closed), tolerance, rule)
}

/** the triangles of [polygons] together: everything that one of them covers, without overlaps */
private fun union(polygons: List<List<Vector2>>): List<Vector2> {
    if (polygons.isEmpty()) return emptyList()
    val tessellator = Tessellator()
    tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE.toDouble())
    tessellator.gluTessNormal(0.0, 0.0, 1.0)
    tessellator.gluTessBeginPolygon(null)
    for (polygon in polygons) {
        if (polygon.size < 3) continue
        // every polygon counter-clockwise, so that the winding numbers of overlaps add up
        val ccw = if (signedArea(polygon) < 0.0) polygon.reversed() else polygon
        tessellator.gluTessBeginContour()
        for (p in ccw) {
            val data = doubleArrayOf(p.x, p.y, 0.0)
            tessellator.gluTessVertex(data, 0, data)
        }
        tessellator.gluTessEndContour()
    }
    tessellator.gluTessEndPolygon()
    return triangles(tessellator)
}

private fun signedArea(polygon: List<Vector2>): Double {
    var a = 0.0
    for (i in polygon.indices) {
        val p = polygon[i]
        val q = polygon[(i + 1) % polygon.size]
        a += p.x * q.y - q.x * p.y
    }
    return a / 2.0
}
//</editor-fold>

//<editor-fold desc="strokes">
/** the normal at the left of this direction, counter-clockwise with y up */
private fun Vector2.left() = Vector2(-y, x)

/** the points of an arc around [center] of [radius] from angle [from] turning by [sweep], [tolerance] from the circle */
private fun arc(center: Vector2, radius: Double, from: Double, sweep: Double, tolerance: Double): List<Vector2> {
    val step = 2.0 * acos((1.0 - tolerance / radius).coerceIn(-1.0, 1.0)).coerceAtLeast(PI / 64.0)
    val n = ceil(abs(sweep) / step).toInt().coerceIn(2, 256)
    return List(n + 1) { i -> val a = from + sweep * i / n; center + Vector2(cos(a), sin(a)) * radius }
}

/**
 * The triangles of a stroke of [weight] along [contours], with [join]s, [cap]s and a [miterLimit] as in SVG: the
 * outlines of the segments of the polylines of the contours, of their joins and of their caps, together
 */
internal fun strokeTriangles(
    contours: List<ShapeContour>, weight: Double, join: org.openrndr.draw.LineJoin, cap: org.openrndr.draw.LineCap, miterLimit: Double, tolerance: Double
): List<Vector2> {
    val half = weight / 2.0
    val polygons = mutableListOf<List<Vector2>>()
    for (contour in contours) {
        if (contour.empty) continue
        val raw = contour.adaptivePositions(tolerance)
        val points = raw.filterIndexed { i, p -> i == 0 || p.squaredDistanceTo(raw[i - 1]) > 1E-18 }
            .let { if (contour.closed && it.size > 1 && it.first().squaredDistanceTo(it.last()) < 1E-18) it.dropLast(1) else it }
        val closed = contour.closed && points.size > 2
        if (points.size < 2) {
            // a dot, where caps draw one
            val p = points.firstOrNull() ?: continue
            when (cap) {
                org.openrndr.draw.LineCap.ROUND -> polygons.add(arc(p, half, 0.0, 2.0 * PI, tolerance).dropLast(1))
                org.openrndr.draw.LineCap.SQUARE -> polygons.add(listOf(p + Vector2(-half, -half), p + Vector2(half, -half), p + Vector2(half, half), p + Vector2(-half, half)))
                else -> {}
            }
            continue
        }
        val n = points.size
        val edges = if (closed) n else n - 1
        fun dir(i: Int) = (points[(i + 1) % n] - points[i]).normalized
        // the segments
        for (i in 0 until edges) {
            val a = points[i]
            val b = points[(i + 1) % n]
            val d = dir(i)
            val nrm = d.left() * half
            polygons.add(listOf(a + nrm, b + nrm, b - nrm, a - nrm))
        }
        // the joins
        val joins = if (closed) (0 until n) else (1 until n - 1)
        for (i in joins) {
            val v = points[i]
            val d0 = dir((i - 1).mod(n))
            val d1 = dir(i % n)
            val cross = d0.x * d1.y - d0.y * d1.x
            val dot = d0.dot(d1)
            if (abs(cross) < 1E-12 && dot > 0.0) continue
            // the outer side of the turn
            val side = if (cross > 0.0) -1.0 else 1.0
            val n0 = d0.left() * side
            val n1 = d1.left() * side
            val o0 = v + n0 * half
            val o1 = v + n1 * half
            when (join) {
                org.openrndr.draw.LineJoin.ROUND -> {
                    val a0 = kotlin.math.atan2(n0.y, n0.x)
                    var sweep = kotlin.math.atan2(n1.y, n1.x) - a0
                    while (sweep > PI) sweep -= 2.0 * PI
                    while (sweep < -PI) sweep += 2.0 * PI
                    polygons.add(listOf(v) + arc(v, half, a0, sweep, tolerance))
                }
                org.openrndr.draw.LineJoin.MITER -> {
                    val bisector = (n0 + n1).let { if (it.length < 1E-12) n0 else it.normalized }
                    val cosHalf = bisector.dot(n0)
                    val miterLength = if (cosHalf > 1E-12) half / cosHalf else Double.POSITIVE_INFINITY
                    if (miterLength * 2.0 / weight <= miterLimit) polygons.add(listOf(v, o0, v + bisector * miterLength, o1))
                    else polygons.add(listOf(v, o0, o1))
                }
                else -> polygons.add(listOf(v, o0, o1))
            }
        }
        // the caps
        if (!closed) {
            for ((p, d) in listOf(points.first() to -dir(0), points.last() to dir(n - 2))) {
                val nrm = d.left() * half
                when (cap) {
                    org.openrndr.draw.LineCap.ROUND -> polygons.add(arc(p, half, kotlin.math.atan2(nrm.y, nrm.x), -PI, tolerance).let { it + listOf(p) })
                    org.openrndr.draw.LineCap.SQUARE -> polygons.add(listOf(p + nrm, p + nrm + d * half, p - nrm + d * half, p - nrm))
                    else -> {}
                }
            }
        }
    }
    return union(polygons)
}
//</editor-fold>

