package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.ShadeStyleFilter1to1
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.shadeStyle
import org.openrndr.shape.Rectangle

/**
 * A stage of an [EffectChain]
 */
sealed interface EffectStage {
    /** reads its input at the coordinates [warp] maps the output coordinates to */
    class Warp(val warp: WarpPreset) : EffectStage

    /** maps every color with [color] */
    class Color(val color: ColorPreset) : EffectStage

    /** applies [filter], which can read its input anywhere, such as a blur */
    class Filter(val filter: Filter1to1) : EffectStage
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

    fun filter(filter: Filter1to1) {
        stages.add(EffectStage.Filter(filter))
    }
}

/**
 * A filter that applies [stages] in order. Consecutive warp and color stages are fused into a single pass that reads
 * the input once, filter stages run as passes of their own. The result is that of applying the stages one after
 * another, without resampling between fused stages.
 *
 * Changing the properties of the presets and filters of the stages takes effect on the next apply, assigning [stages]
 * builds the passes again; shaders are shared by chains with the same kinds and classes of stages.
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

    private val intermediates = arrayOfNulls<ColorBuffer>(2)

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        val passes = passes
        if (passes.isEmpty()) {
            source[0].copyTo(target[0])
            return
        }
        var input = source[0]
        for ((index, pass) in passes.withIndex()) {
            val output = if (index == passes.lastIndex) target[0] else intermediate(index % 2, target[0])
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
         * coordinates are found from the last stage to the first, the colors from the first to the last.
         */
        fun fuse(stages: List<EffectStage>): Filter1to1 {
            val code = buildString {
                appendLine("vec2 u${stages.size} = c_textureCoordinate;")
                for (k in stages.indices.reversed()) {
                    when (stages[k]) {
                        is EffectStage.Warp -> appendLine("vec2 u$k = p_s$k(u${k + 1});")
                        else -> appendLine("vec2 u$k = u${k + 1};")
                    }
                }
                appendLine("vec4 c = texture(p_input0, u0);")
                for (k in stages.indices) {
                    if (stages[k] is EffectStage.Color) {
                        appendLine("c = p_s$k(c, u${k + 1});")
                    }
                }
                append("x_fill = c;")
            }
            val filter = ShadeStyleFilter1to1(shadeStyle { fragmentTransform = code })
            for ((k, stage) in stages.withIndex()) {
                val preset: Preset = when (stage) {
                    is EffectStage.Warp -> stage.warp
                    is EffectStage.Color -> stage.color
                    is EffectStage.Filter -> error("filters are not fused")
                }
                require(preset.contract.accepts(preset.dynamicParameter)) {
                    "stage $k: ${preset::class.simpleName} does not provide a ${preset.contract}"
                }
                filter.parameter("s$k", preset)
            }
            return filter
        }
    }
}

/**
 * An [EffectChain] with the stages that [builder] adds
 */
fun effectChain(builder: EffectChainBuilder.() -> Unit): EffectChain =
    EffectChain(EffectChainBuilder().apply(builder).stages.toList())
