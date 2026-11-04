@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector4

/**
 * A sampler: a function `vec4 sample(sampler2D image, vec2 uv)` that reads an image at and around a texture
 * coordinate. In an [EffectChain] a sampler reads the image as it is after the stages before it, which takes a
 * pass of its own.
 */
abstract class SamplerPreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec4", "sampler2D", "vec2")
    }
}

/**
 * Chromatic aberration: reads red [amount] pixels to the left and blue [amount] pixels to the right
 */
class ChromaticAberration : SamplerPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "sampler2D image, vec2 uv", """
        vec2 offset = vec2(p_amount / float(textureSize(image, 0).x), 0.0);
        vec4 c = texture(image, uv);
        return vec4(texture(image, uv - offset).r, c.g, texture(image, uv + offset).b, c.a);
        """.trimIndent()
    )

    @DoubleParameter("amount", 0.0, 32.0)
    var amount: Double by dynamicParameter.Parameter("amount", 8.0)
}
