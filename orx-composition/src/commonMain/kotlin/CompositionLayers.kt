package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorBufferAttachment
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.shadestyles.presets.boundsTransform
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle

/*
 * Layers: nodes that are drawn on their own before they are composited with what is under them, with an opacity, a
 * mask and a blend mode. A layer is a render target of the size of the target that is drawn to, drawn with the same
 * transforms, so its pixels are where the pixels of the target are.
 */

/**
 * Render targets by nesting depth, for layers and for the masks of layers. Their colors are 16-bit floats, so faint
 * edges keep their colors when they are composited; the depth buffer holds the stencil that shapes are filled with.
 */
private class TargetPool {
    private val targets = mutableListOf<RenderTarget>()

    /** the target at [depth] with the size and content scale of [like] */
    fun get(depth: Int, like: RenderTarget): RenderTarget {
        while (targets.size <= depth) targets.add(create(like))
        val target = targets[depth]
        if (target.width == like.width && target.height == like.height && target.contentScale == like.contentScale) {
            return target
        }
        target.colorBuffer(0).destroy()
        target.detachColorAttachments()
        target.depthBuffer?.destroy()
        target.detachDepthBuffer()
        target.destroy()
        return create(like).also { targets[depth] = it }
    }

    private fun create(like: RenderTarget) = renderTarget(like.width, like.height, contentScale = like.contentScale) {
        colorBuffer(type = ColorType.FLOAT16)
        depthBuffer()
    }.also { it.colorBuffer(0).filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST) }
}

private val layerTargets = TargetPool()
private val maskTargets = TargetPool()

/** the depth of the layer that is drawn to, nested compositions continue the depth */
private var layerDepth = 0

/** the copy of the target that a layer is blended with, reused while its size matches */
private var backdropBuffer: ColorBuffer? = null

/** a texture that stands in for a mask or a backdrop that is not used */
private val unusedTexture by lazy { colorBuffer(1, 1) }

/**
 * Composites a layer: multiplies its alpha by [opacity] and by the value of the mask, and blends it with the backdrop
 * by the mode, using the formulas of the W3C compositing and blending specification. The layer, the mask and the
 * backdrop have the size of the target, they are read at the fragment position.
 */
private class LayerStyle : ShadeStyle() {
    init {
        fragmentPreamble = """
            vec3 layer_soft(vec3 b, vec3 s) {
                vec3 d = mix(sqrt(b), ((16.0 * b - 12.0) * b + 4.0) * b, step(b, vec3(0.25)));
                return mix(b + (2.0 * s - 1.0) * (d - b), b - (1.0 - 2.0 * s) * b * (1.0 - b), step(s, vec3(0.5)));
            }
            vec3 layer_hard(vec3 b, vec3 s) {
                return mix(1.0 - 2.0 * (1.0 - s) * (1.0 - b), 2.0 * s * b, step(s, vec3(0.5)));
            }
            float layer_lum(vec3 c) { return dot(c, vec3(0.3, 0.59, 0.11)); }
            vec3 layer_clip(vec3 c) {
                float l = layer_lum(c);
                float n = min(c.r, min(c.g, c.b));
                float x = max(c.r, max(c.g, c.b));
                if (n < 0.0) c = l + (c - l) * l / max(l - n, 1e-6);
                if (x > 1.0) c = l + (c - l) * (1.0 - l) / max(x - l, 1e-6);
                return c;
            }
            vec3 layer_setLum(vec3 c, float l) { return layer_clip(c + (l - layer_lum(c))); }
            float layer_sat(vec3 c) { return max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b)); }
            vec3 layer_setSat(vec3 c, float s) {
                float n = min(c.r, min(c.g, c.b));
                float x = max(c.r, max(c.g, c.b));
                return x > n ? (c - n) * s / (x - n) : vec3(0.0);
            }
            vec3 layer_toSrgb(vec3 c) {
                c = clamp(c, 0.0, 1.0);
                return mix(1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, c * 12.92, step(c, vec3(0.0031308)));
            }
            vec3 layer_toLinear(vec3 c) {
                return mix(pow((c + 0.055) / 1.055, vec3(2.4)), c / 12.92, step(c, vec3(0.04045)));
            }
            vec3 layer_blend(int mode, vec3 b, vec3 s) {
                if (mode == 1) return b * s;
                if (mode == 2) return b + s - b * s;
                if (mode == 3) return layer_hard(s, b);
                if (mode == 4) return min(b, s);
                if (mode == 5) return max(b, s);
                if (mode == 6) return mix(min(vec3(1.0), b / max(1.0 - s, 1e-6)), vec3(1.0), step(1.0, s)) * step(1e-6, b);
                if (mode == 7) return mix(1.0 - min(vec3(1.0), (1.0 - b) / max(s, 1e-6)), vec3(0.0), step(s, vec3(0.0))) * (1.0 - step(1.0 - 1e-6, b)) + step(1.0 - 1e-6, b);
                if (mode == 8) return layer_hard(b, s);
                if (mode == 9) return layer_soft(b, s);
                if (mode == 10) return abs(b - s);
                if (mode == 11) return b + s - 2.0 * b * s;
                if (mode == 12) return layer_setLum(layer_setSat(s, layer_sat(b)), layer_lum(b));
                if (mode == 13) return layer_setLum(layer_setSat(b, layer_sat(s)), layer_lum(b));
                if (mode == 14) return layer_setLum(s, layer_lum(b));
                if (mode == 15) return layer_setLum(b, layer_lum(s));
                return s;
            }
        """.trimIndent()
        fragmentTransform = """
            vec2 layer_uv = gl_FragCoord.xy / vec2(textureSize(p_backdrop, 0));
            // the samples are in linear light, the luminance of masks and the blend modes are on sRGB values
            float layer_mask = 1.0;
            if (p_maskType == 1) {
                vec4 m = texture(p_mask, gl_FragCoord.xy / vec2(textureSize(p_mask, 0)));
                vec3 c = layer_toSrgb(m.a > 0.0 ? m.rgb / m.a : vec3(0.0));
                layer_mask = dot(c, vec3(0.2125, 0.7154, 0.0721)) * m.a;
            } else if (p_maskType == 2) {
                layer_mask = texture(p_mask, gl_FragCoord.xy / vec2(textureSize(p_mask, 0))).a;
            }
            float as = x_fill.a * p_opacity * layer_mask;
            if (p_mode == 0) {
                x_fill.a = as;
            } else {
                vec4 backdrop = texture(p_backdrop, layer_uv);
                float ab = backdrop.a;
                vec3 cb = layer_toSrgb(ab > 0.0 ? backdrop.rgb / ab : vec3(0.0));
                vec3 cs = layer_toSrgb(x_fill.rgb);
                vec3 co;
                float ao;
                if (p_mode == 16) {
                    co = min(cs * as + cb * ab, vec3(1.0));
                    ao = min(as + ab, 1.0);
                } else {
                    vec3 mixed = (1.0 - ab) * cs + ab * layer_blend(p_mode, cb, cs);
                    co = as * mixed + (1.0 - as) * ab * cb;
                    ao = as + ab * (1.0 - as);
                }
                x_fill = vec4(layer_toLinear(ao > 0.0 ? co / ao : vec3(0.0)), ao);
            }
        """.trimIndent()
    }

    fun set(opacity: Double, mask: ColorBuffer?, maskType: MaskType, mode: MixBlendMode, backdrop: ColorBuffer?) {
        parameter("opacity", opacity)
        parameter("maskType", if (mask == null) 0 else if (maskType == MaskType.LUMINANCE) 1 else 2)
        parameter("mask", mask ?: unusedTexture)
        parameter("mode", mode.ordinal)
        parameter("backdrop", backdrop ?: unusedTexture)
    }
}

private val layerStyle by lazy { LayerStyle() }

/** true when [node], drawn with [opacity], is drawn on a layer of its own */
internal fun needsLayer(node: CompositionNode, opacity: Double): Boolean {
    val style = node.style
    if (style.mask !is MaskReference.None || style.mixBlendMode != MixBlendMode.NORMAL) return true
    return when (node) {
        is GroupNode -> opacity < 1.0 || style.isolation == Isolation.ISOLATE
        // the stroke and the markers of a translucent shape do not show the fill under them
        is ShapeNode -> opacity < 1.0 && node.effectiveStyle.let { s ->
            s.fill !is Paint.None && (s.stroke !is Paint.None || s.markerStart.value != null ||
                    s.markerMid.value != null || s.markerEnd.value != null)
        }
        else -> false
    }
}

/** true when nodes of [node] are blended with a mode, the composition is then drawn on a layer of its own */
internal fun usesBlendModes(node: CompositionNode): Boolean =
    node.style.mixBlendMode != MixBlendMode.NORMAL || (node is GroupNode && node.children.any(::usesBlendModes))

/**
 * Draws [content] on a layer and composites it with [opacity], the mask and the blend mode of [node]
 */
internal fun Drawer.drawLayer(node: CompositionNode, opacity: Double, content: () -> Unit) {
    val active = RenderTarget.active
    val layer = layerTargets.get(layerDepth, active)
    layerDepth++
    try {
        isolatedWithTarget(layer) {
            clear(ColorRGBa.TRANSPARENT)
            content()
        }
        val mask = node.style.mask.value
        val maskBuffer = mask?.let { renderMask(it, node, active) ?: return }
        val mode = node.style.mixBlendMode
        val backdrop = if (mode == MixBlendMode.NORMAL) null else {
            // the backdrop is a copy of the target, a texture can not be read while it is drawn to
            val target = (active.colorAttachments.firstOrNull() as? ColorBufferAttachment)?.colorBuffer
            if (target == null) null else {
                val buffer = backdropBuffer?.takeIf {
                    it.width == target.width && it.height == target.height && it.contentScale == target.contentScale &&
                            it.type == target.type && it.format == target.format
                } ?: colorBuffer(target.width, target.height, target.contentScale, target.format, target.type)
                    .also { backdropBuffer?.destroy(); backdropBuffer = it }
                target.copyTo(buffer)
                buffer
            }
        }
        composite(layer.colorBuffer(0), opacity, maskBuffer, mask?.type ?: MaskType.LUMINANCE,
            if (backdrop == null) MixBlendMode.NORMAL else mode, backdrop)
    } finally {
        layerDepth--
    }
}

/** draws [layer] over the whole active target with the layer style */
private fun Drawer.composite(
    layer: ColorBuffer, opacity: Double, mask: ColorBuffer?, maskType: MaskType, mode: MixBlendMode,
    backdrop: ColorBuffer?
) {
    isolated {
        val active = RenderTarget.active
        ortho(active)
        view = Matrix44.IDENTITY
        model = Matrix44.IDENTITY
        layerStyle.set(opacity, mask, maskType, mode, backdrop)
        shadeStyle = layerStyle
        // a blended layer replaces the backdrop that it is blended with
        drawStyle.blendMode = if (backdrop != null) BlendMode.REPLACE else BlendMode.OVER
        drawStyle.colorMatrix = org.openrndr.math.Matrix55.IDENTITY
        image(layer, 0.0, 0.0, active.width.toDouble(), active.height.toDouble())
    }
}

/**
 * Renders the content of [mask] for [node] with the current transforms, in its region. Null when nothing of the node
 * is drawn: when the mask is over the bounds of a node without bounds.
 */
private fun Drawer.renderMask(mask: Mask, node: CompositionNode, like: RenderTarget): ColorBuffer? {
    val bounds = localBounds(node)
    val usable = bounds != null && bounds.width > 0.0 && bounds.height > 0.0
    if ((mask.units == PaintUnits.BOUNDS || (mask.region != null && mask.regionUnits == PaintUnits.BOUNDS)) && !usable) {
        return null
    }
    // the content in its units, clipped to the region
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
    val target = maskTargets.get(layerDepth - 1, like)
    isolatedWithTarget(target) {
        clear(ColorRGBa.TRANSPARENT)
        composition(Composition(root))
    }
    // the children of the mask are not moved to the temporary groups
    mask.content.children.forEach { it.parent = mask.content }
    return target.colorBuffer(0)
}
