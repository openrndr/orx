package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.glslLibrary
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.ShadeStyleFilter1to1
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * A stage of an [EffectChain]
 */
sealed interface EffectStage {
    /** reads its input at the coordinates [warp] maps the output coordinates to */
    class Warp(val warp: WarpPreset) : EffectStage

    /** maps every color with [color] */
    class Color(val color: ColorPreset) : EffectStage

    /** replaces the image by the colors that [source] generates */
    class Source(val source: SourcePreset) : EffectStage

    /** reads the image with [sampler], which can read around the coordinate, it starts a pass */
    class Sample(val sampler: SamplerPreset) : EffectStage

    /** applies [filter], which can read its input anywhere, such as a blur */
    class Filter(val filter: Filter1to1) : EffectStage

    /**
     * combines the image, the base, with the color of [layer] at the same coordinate by [blend], like a color stage:
     * `blend(base, layer)`
     */
    class Blend(val blend: BlendPreset, val layer: BlendLayer) : EffectStage
}

/**
 * The layer of a [EffectStage.Blend]
 */
sealed interface BlendLayer {
    /** the input of the chain, before any of its stages */
    data object Input : BlendLayer

    /** the colors of [image] */
    class Image(val image: ColorBuffer) : BlendLayer

    /** the colors that [source] generates */
    class Source(val source: SourcePreset) : BlendLayer
}

/**
 * Builds the stages of an [EffectChain]
 */
class EffectChainBuilder {
    internal val stages = mutableListOf<EffectStage>()

    fun warp(warp: WarpPreset) {
        stages.add(EffectStage.Warp(warp))
    }

    fun color(color: ColorPreset) {
        stages.add(EffectStage.Color(color))
    }

    fun source(source: SourcePreset) {
        stages.add(EffectStage.Source(source))
    }

    fun sample(sampler: SamplerPreset) {
        stages.add(EffectStage.Sample(sampler))
    }

    fun filter(filter: Filter1to1) {
        stages.add(EffectStage.Filter(filter))
    }

    /** blends the image with [layer] by [blend], see [EffectStage.Blend] */
    fun blend(blend: BlendPreset, layer: BlendLayer) {
        stages.add(EffectStage.Blend(blend, layer))
    }
}

/**
 * A filter that applies [stages] in order. Consecutive warp, color, source and blend stages are fused into a single pass
 * that reads the input once, filter stages run as passes of their own. A blend stage with the input of the chain as its
 * layer reads the input in whatever pass it is fused into. A source stage replaces the image, the stages
 * before it have no effect. A sample stage reads the image as it is after the stages before it, it starts a pass
 * that reads the input with the sampler. The result is that of applying the stages one after
 * another, without resampling between fused stages.
 *
 * Changing the properties of the presets and filters of the stages takes effect on the next apply, assigning [stages]
 * builds the passes again; shaders are shared by chains with the same kinds and classes of stages. Every apply sets the
 * values of [fxContextLibrary]: [time], [frame] and the size of the image, also for [PresetFilter] stages.
 *
 * ```
 * val chain = effectChain {
 *     warp(Ripple())
 *     color(Grade())
 *     filter(HashBlur())
 *     color(Vignette())
 * }
 * chain.apply(image, result)
 * ```
 */
class EffectChain(stages: List<EffectStage> = emptyList()) : Filter1to1(null) {
    /** the stages, in the order in which they are applied */
    var stages: List<EffectStage> = stages
        set(value) {
            field = value
            passes = buildPasses(value)
        }

    private var passes: List<Filter1to1> = buildPasses(stages)

    /** the number of passes the stages are applied in */
    val passCount: Int get() = passes.size

    /** the time in seconds, for presets that read `fx_time()`, see [fxContextLibrary] */
    var time = 0.0

    /** the number of the frame, for presets that read `fx_frame()`, see [fxContextLibrary] */
    var frame = 0

    private val intermediates = arrayOfNulls<ColorBuffer>(2)

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        val passes = passes
        if (passes.isEmpty()) {
            source[0].copyTo(target[0])
            return
        }
        val resolution = Vector2(target[0].effectiveWidth.toDouble(), target[0].effectiveHeight.toDouble())
        var input = source[0]
        for ((index, pass) in passes.withIndex()) {
            val output = if (index == passes.lastIndex) target[0] else intermediate(index % 2, target[0])
            when (pass) {
                is FusedPass -> {
                    pass.setFxContext(time, frame, resolution)
                    if (pass.readsInput) pass.parameter("chainInput", source[0])
                }
                is PresetFilter -> pass.setFxContext(time, frame, resolution)
            }
            pass.apply(input, output, clip)
            input = output
        }
    }

    /** the intermediate buffer [index], made again when it does not match [target] */
    private fun intermediate(index: Int, target: ColorBuffer): ColorBuffer {
        val current = intermediates[index]
        if (current != null && current.width == target.width && current.height == target.height &&
            current.format == target.format && current.type == target.type
        ) return current
        current?.destroy()
        return target.createEquivalent().also { intermediates[index] = it }
    }

    /** destroys the intermediate buffers, the filters of the stages are not destroyed */
    override fun destroy() {
        super.destroy()
        for (i in intermediates.indices) {
            intermediates[i]?.destroy()
            intermediates[i] = null
        }
    }

    private companion object {
        fun buildPasses(stages: List<EffectStage>): List<Filter1to1> {
            val passes = mutableListOf<Filter1to1>()
            val run = mutableListOf<EffectStage>()
            for (stage in stages) {
                if (stage is EffectStage.Filter) {
                    if (run.isNotEmpty()) passes.add(fuse(run.toList()))
                    run.clear()
                    passes.add(stage.filter)
                } else if (stage is EffectStage.Sample) {
                    if (run.isNotEmpty()) passes.add(fuse(run.toList()))
                    run.clear()
                    run.add(stage)
                } else {
                    run.add(stage)
                }
            }
            if (run.isNotEmpty()) passes.add(fuse(run))
            return passes
        }

        /**
         * A single pass for warp and color [stages]. Applied one after another, the color of stage k reads the
         * output of stage k - 1 at the coordinate that the later warps map the output coordinate to, so the
         * coordinates are found from the last stage to the first, the colors from the first to the last. The colors
         * start at the last source stage, or else at the input, read by the sample stage that starts the run or by
         * `texture`.
         */
        fun fuse(stages: List<EffectStage>): FusedPass {
            val lastSource = stages.indexOfLast { it is EffectStage.Source }
            val code = buildString {
                appendLine("vec2 u${stages.size} = c_textureCoordinate;")
                for (k in stages.indices.reversed()) {
                    when (stages[k]) {
                        is EffectStage.Warp -> appendLine("vec2 u$k = p_s$k(u${k + 1});")
                        else -> appendLine("vec2 u$k = u${k + 1};")
                    }
                }
                // the images hold premultiplied colors, the stages work on straight colors, which the filter
                // premultiplies when it writes them
                appendLine("vec4 c;")
                if (lastSource >= 0) {
                    appendLine("c = p_s$lastSource(u${lastSource + 1});")
                } else if (stages.first() is EffectStage.Sample) {
                    appendLine("c = straight(p_s0(p_input0, u1));")
                } else {
                    appendLine("c = straight(texture(p_input0, u0));")
                }
                for (k in lastSource + 1 until stages.size) {
                    when (val stage = stages[k]) {
                        is EffectStage.Color -> appendLine("c = p_s$k(c, u${k + 1});")
                        is EffectStage.Blend -> {
                            val layer = when (stage.layer) {
                                BlendLayer.Input -> "straight(texture(p_chainInput, u${k + 1}))"
                                is BlendLayer.Image -> "straight(texture(p_l$k, u${k + 1}))"
                                is BlendLayer.Source -> "p_l$k(u${k + 1})"
                            }
                            appendLine("c = p_s$k(c, $layer);")
                        }
                        else -> {}
                    }
                }
                appendLine("x_fill = c;")
                append(FX_CONTEXT_USE)
            }
            val readsInput = stages.withIndex().any { (k, stage) ->
                k > lastSource && stage is EffectStage.Blend && stage.layer == BlendLayer.Input
            }
            val filter = FusedPass(shadeStyle { libraries = listOf(straightColorLibrary); fragmentTransform = code }, readsInput)
            filter.setFxContext(0.0, 0, Vector2(1.0, 1.0))
            for ((k, stage) in stages.withIndex()) {
                val preset: Preset = when (stage) {
                    is EffectStage.Warp -> stage.warp
                    is EffectStage.Color -> stage.color
                    is EffectStage.Source -> stage.source
                    is EffectStage.Sample -> stage.sampler
                    is EffectStage.Blend -> stage.blend
                    is EffectStage.Filter -> error("filters are not fused")
                }
                require(preset.contract.accepts(preset.dynamicParameter)) {
                    "stage $k: ${preset::class.simpleName} does not provide a ${preset.contract}"
                }
                filter.parameter("s$k", preset)
                if (stage is EffectStage.Blend) when (val layer = stage.layer) {
                    is BlendLayer.Image -> filter.parameter("l$k", layer.image)
                    is BlendLayer.Source -> {
                        require(layer.source.contract.accepts(layer.source.dynamicParameter)) {
                            "stage $k: ${layer.source::class.simpleName} does not provide a ${layer.source.contract}"
                        }
                        filter.parameter("l$k", layer.source)
                    }
                    BlendLayer.Input -> {}
                }
            }
            // the input of the chain is bound on every apply, a placeholder until then
            if (readsInput) filter.parameter("chainInput", placeholder)
            return filter
        }

        /** an image to bind until the input of the chain is */
        private val placeholder: ColorBuffer by lazy { colorBuffer(1, 1) }
    }
}

/**
 * `vec4 straight(vec4 c)`: the straight color of [c], a premultiplied color as images hold them; filters that read
 * images and write their colors to `x_fill` need straight colors, the filter premultiplies them when it writes them
 */
val straightColorLibrary = glslLibrary("straight_color", "vec4 straight(vec4 c) { return c.a > 0.0 ? vec4(c.rgb / c.a, c.a) : c; }")

/** a fused pass of an [EffectChain], which [readsInput] when it has a blend stage with the input of the chain */
private class FusedPass(style: org.openrndr.draw.ShadeStyle, val readsInput: Boolean) : ShadeStyleFilter1to1(style)

/**
 * An [EffectChain] with the stages that [builder] adds
 */
fun effectChain(builder: EffectChainBuilder.() -> Unit): EffectChain =
    EffectChain(EffectChainBuilder().apply(builder).stages.toList())
